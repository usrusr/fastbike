package de.ulf_schreiber.fastbike.desktop;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import de.ulf_schreiber.fastbike.boundingpiecechain.Route;
import de.ulf_schreiber.fastbike.core.BrouterClient;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamException;

import static com.fasterxml.aalto.AsyncXMLStreamReader.END_DOCUMENT;
import static com.fasterxml.aalto.AsyncXMLStreamReader.EVENT_INCOMPLETE;

public class BrouterClientDesktop implements BrouterClient {


    private final static String UTF_8 = StandardCharsets.UTF_8.name();
    private final String baseUrl;
    private final HttpClient<ByteBuf, ByteBuf> client;
    private final Scheduler scheduler = Schedulers.io();
    private final AsyncXMLInputFactory aaltoFact = new InputFactoryImpl();
    String profile = "fastbike";

    public BrouterClientDesktop(String host, int port) {
        this.client = HttpClient.newClient(host, port)
                .readTimeOut(600, TimeUnit.SECONDS)
//            .flatMap(resp -> resp.getContent().map(bb -> bb.toString(Charset.defaultCharset())))

        ;
        this.baseUrl = "http://" + host + ":" + port;

    }

    @Override
    public Cancellable call(double fromLat, double fromLon, double toLat, double toLon, OnRoute resultHandler) {
        //param params--> Map of params:
        //  "pathToFileResult"-->String with the path to where the result must be saved, including file name and extension
        //                    -->if null, the track is passed via the return argument
        //  "maxRunningTime"-->String with a number of seconds for the routing timeout, default = 60
        //  "turnInstructionFormat"-->String selecting the format for turn-instructions values: osmand, locus
        //  "trackFormat"-->[kml|gpx] default = gpx
        //  "lats"-->double[] array of latitudes; 2 values at least.
        //  "lons"-->double[] array of longitudes; 2 values at least.
        //  "nogoLats"-->double[] array of nogo latitudes; may be null.
        //  "nogoLons"-->double[] array of nogo longitudes; may be null.
        //  "nogoRadi"-->double[] array of nogo radius in meters; may be null.
        //  "fast"-->[0|1]
        //  "v"-->[motorcar|bicycle|foot]
        //  "remoteProfile"--> (String), net-content of a profile. If remoteProfile != null, v+fast are ignored
        //return null if all ok and no path given, the track if ok and path given, an error message if it was wrong
        //call in a background thread, heavy task!


        // ?lonlats=10.929337,49.531894|10.876465,49.545707&nogos=&profile=trekking&alternativeidx=0&format=geojson

        StringBuilder sb = new StringBuilder("/brouter");
        sb.append("?lonlats=")
                .append(fromLon)
                .append(fromLat)
                .append("|")
                .append(toLon)
                .append(toLat)
        ;
        sb.append("&profile=").append(encode(profile));
        sb.append("&alternativeidx=").append(encode("" + 1));
        sb.append("&format=gpx");

        return new RouteReq(sb.toString(), resultHandler);
    }

    private String encode(String profile) {
        try {
            return URLEncoder.encode(profile, UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return profile;
        }
    }


    public class RouteReq implements Cancellable {


        private Subscription subscription;

        public RouteReq(String absoluteUrlWithoutHost, OnRoute resultHandler) {


            HttpClientRequest<ByteBuf, ByteBuf> req = client.createGet(absoluteUrlWithoutHost);


            AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = aaltoFact.createAsyncForByteBuffer();


//            Observable<String> obs = req
//                    .flatMap(resp -> resp.getContent().map(bb -> bb.toString(StandardCharsets.UTF_8)))
//                    ;
            Route route = resultHandler.getRoute();


            Observable<ByteBuf> contentChunks = req.flatMap(HttpClientResponse::getContent);
            Observable<ArrayList<Route.Reading>> readPoints = contentChunks.reduce(new ArrayList<Route.Reading>(), (ArrayList<Route.Reading> list, ByteBuf buf) -> {

//            this.subscription = contentChunks.subscribe(buf -> {
                        try {
                            ByteBuffer b = buf.nioBuffer();
                            parser.getInputFeeder().feedInput(b);

                            StringBuilder sb = new StringBuilder();
                            boolean isTrkpt = false;
                            boolean isElev = false;
                            double ele = 0;
                            double lat = Double.NaN;
                            double lon = Double.NaN;
                            double lastLat = Double.NaN;
                            double lastLon = Double.NaN;
                            double lastEle = Double.NaN;
                            while (parser.hasNext()) {
                                int event = parser.next();
                                if (event == END_DOCUMENT || event == EVENT_INCOMPLETE) {
                                    break;
                                } else if (event == AsyncXMLStreamReader.START_ELEMENT) {
                                    if (isTrkpt) {
                                        if ("ele".equals(parser.getLocalName())) {
                                            isElev = true;
                                            sb.setLength(0);
                                        }
                                    } else if ("trkpt".equals(parser.getLocalName())) {
                                        isTrkpt = true;
                                        lon = Double.parseDouble(parser.getAttributeValue(null, "lon"));
                                        lat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
                                        sb.setLength(0);
                                    }
                                } else if (event == AsyncXMLStreamReader.END_ELEMENT) {
                                    if (isElev && "ele".equals(parser.getLocalName())) {
                                        ele = Double.parseDouble(sb.toString());
                                        isElev = false;
                                    } else if (isTrkpt && "trkpt".equals(parser.getLocalName())) {


                                        double distance;
                                        if(Double.isNaN(lastLat)){
                                            distance = 0;
                                        } else {
                                            distance = route.calculateDistance(lat, lon, ele, lastLat, lastLon, lastEle);
                                        }
                                        Route.Reading val = route.immutable(lat, lon, distance, ele);

                                        list.add(val);
                                    }
                                } else if (event == AsyncXMLStreamReader.CHARACTERS) {
                                    if (isElev) {
                                        sb.append(parser.getText().trim());
                                    }
                                }
                            }

                        } catch (XMLStreamException e) {
                            Exceptions.propagate(e);
                        }
                        return list;
                    }
            );

            this.subscription = readPoints.subscribeOn(scheduler).subscribe(resultHandler::success, resultHandler::fail);

        }

        @Override
        public void cancel() {
            if(subscription!=null) {
                subscription.unsubscribe();
                subscription=null;
            }
        }
    }
}
