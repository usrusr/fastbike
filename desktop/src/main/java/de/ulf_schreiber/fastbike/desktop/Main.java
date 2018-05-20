package de.ulf_schreiber.fastbike.desktop;


import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.input.MapViewComponentListener;
import org.mapsforge.map.awt.input.MouseEventListener;
import org.mapsforge.map.awt.util.AwtUtil;
import org.mapsforge.map.awt.util.JavaPreferences;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.hills.DiffuseLightShadingAlgorithm;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.util.MapViewProjection;

import java.awt.Dimension;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

public final class Main {
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final boolean SHOW_DEBUG_LAYERS = false;
    private static final boolean SHOW_RASTER_MAP = false;

    private static final String MESSAGE = "Are you sure you want to exit the application?";
    private static final String TITLE = "Confirm close";

    /**
     * Starts the {@code Samples}.
     *
     * @param args command line args: expects the map files as multiple parameters
     *             with possible SRTM hgt folder as 1st argument.
     */
    public static void main(String[] args) throws IOException {
        // Frame buffer HA2
        Parameters.FRAME_BUFFER_HA2 = true;

        // Multithreaded map rendering
        Parameters.NUMBER_OF_THREADS = 2;

        // Square frame buffer
        Parameters.SQUARE_FRAME_BUFFER = false;

        HillsRenderConfig hillsCfg = null;

        File file = new File("local/run/settings.json");
        Settings settings = Settings.load(file);
        Settings.store(file, settings);


        if(settings.demFolder!=null) {
            File demFolder = new File(settings.demFolder);
            if (demFolder != null && demFolder.exists()) {
                MemoryCachingHgtReaderTileSource tileSource = new MemoryCachingHgtReaderTileSource(demFolder, new DiffuseLightShadingAlgorithm(), AwtGraphicFactory.INSTANCE);
                tileSource.setEnableInterpolationOverlap(true);
                hillsCfg = new HillsRenderConfig(tileSource);
                hillsCfg.indexOnThread();
            }
        }

        List<File> mapFiles = getMapFiles(new File(settings.mapsFolder));
        final MapView mapView = createMapView();
        final BoundingBox boundingBox = addLayers(mapView, mapFiles, hillsCfg);

        final PreferencesFacade preferencesFacade = new JavaPreferences(Preferences.userNodeForPackage(Main.class));

        final JFrame frame = new JFrame();
        frame.setTitle("Mapsforge Samples");
        frame.add(mapView);
        frame.pack();
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(frame, MESSAGE, TITLE, JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    mapView.getModel().save(preferencesFacade);
                    mapView.destroyAll();
                    AwtGraphicFactory.clearResourceMemoryCache();
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                final Model model = mapView.getModel();
                model.init(preferencesFacade);
                if (model.mapViewPosition.getZoomLevel() == 0 || !boundingBox.contains(model.mapViewPosition.getCenter())) {
                    byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox, model.displayModel.getTileSize());
                    model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
                }
            }
        });
        frame.setVisible(true);
    }

    private static BoundingBox addLayers(MapView mapView, List<File> mapFiles, HillsRenderConfig hillsRenderConfig) {
        Layers layers = mapView.getLayerManager().getLayers();

        int tileSize = SHOW_RASTER_MAP ? 256 : 512;

        // Tile cache
        TileCache tileCache = AwtUtil.createTileCache(
                tileSize,
                mapView.getModel().frameBufferModel.getOverdrawFactor(),
                1024,
                new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()));

        final BoundingBox boundingBox;

        // Vector
        mapView.getModel().displayModel.setFixedTileSize(tileSize);
        MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        for (File file : mapFiles) {
            mapDataStore.addMapDataStore(new MapFile(file), false, false);
        }
        TileRendererLayer tileRendererLayer = createTileRendererLayer(tileCache, mapDataStore, mapView.getModel().mapViewPosition, hillsRenderConfig);
        layers.add(tileRendererLayer);
        boundingBox = mapDataStore.boundingBox();

        // Debug
        if (SHOW_DEBUG_LAYERS) {
            layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
            layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
        }

        return boundingBox;
    }

    private static MapView createMapView() {
        final MapView mapView = new MapView(){

            @Override public void addListeners() {
                addComponentListener(new MapViewComponentListener(this));
                final MapView mapView = this;
                final MapViewProjection projection = new MapViewProjection(this);
                MouseEventListener mouseEventListener = new MouseEventListener(this) {

                    @Override public void mouseWheelMoved(MouseWheelEvent e) {
                        byte zoomLevelDiff = (byte) -e.getWheelRotation();
                        Model model = mapView.getModel();
                        MapViewPosition mapViewPosition = model.mapViewPosition;

                        Point center = mapView.getModel().mapViewDimension.getDimension().getCenter();
                        double pow = Math.pow(2, zoomLevelDiff);
                        double xDiff = center.x - e.getX();
                        double yDiff = center.y - e.getY();
                        float signum = Math.signum(zoomLevelDiff);
                        double moveHorizontal;
                        double moveVertical;
                        if(zoomLevelDiff>0) {
                            moveHorizontal = xDiff / pow * signum;
                            moveVertical = yDiff / pow * signum;
                        }else{
                            moveHorizontal = xDiff * (pow) * signum*2;
                            moveVertical = yDiff * (pow) * signum*2;
                        }
                        LatLong pivot = mapView.getMapViewProjection().fromPixels(e.getX(), e.getY());
                        if (pivot != null) {
                            mapViewPosition.setPivot(pivot);
                            mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
                        }
                    }
                };
                addMouseListener(mouseEventListener);
                addMouseMotionListener(mouseEventListener);
                addMouseWheelListener(mouseEventListener);
            }
        };
        mapView.getMapScaleBar().setVisible(true);
        if (SHOW_DEBUG_LAYERS) {
            mapView.getFpsCounter().setVisible(true);
        }

        return mapView;
    }

    @SuppressWarnings("unused")
    private static TileDownloadLayer createTileDownloadLayer(TileCache tileCache, MapViewPosition mapViewPosition, TileSource tileSource) {
        return new TileDownloadLayer(tileCache, mapViewPosition, tileSource, GRAPHIC_FACTORY) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                System.out.println("Tap on: " + tapLatLong);
                return true;
            }
        };
    }

    private static TileRendererLayer createTileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, MapViewPosition mapViewPosition, HillsRenderConfig hillsRenderConfig) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, mapViewPosition, false, true, false, GRAPHIC_FACTORY, hillsRenderConfig) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                System.out.println("Tap on: " + tapLatLong);
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);
        return tileRendererLayer;
    }


    private static List<File> getMapFiles(File mapsFolder) {
        List<File> result = new ArrayList<>();
        for (String arg : Objects.requireNonNull(mapsFolder.list(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".map");
            }
        }))) {
            File mapFile = new File(mapsFolder, arg);
            if (!mapFile.exists()) {
                throw new IllegalArgumentException("file does not exist: " + mapFile);
            } else if (!mapFile.isFile()) {
                throw new IllegalArgumentException("not a file: " + mapFile);
            } else if (!mapFile.canRead()) {
                throw new IllegalArgumentException("cannot read file: " + mapFile);
            }
            result.add(mapFile);
        }
        return result;
    }

    private Main() {
        throw new IllegalStateException();
    }
}
