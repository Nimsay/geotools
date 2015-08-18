/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.renderer.lite;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static org.junit.Assert.fail;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.RenderListener;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Style;
import org.geotools.test.TestData;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Z-order rendering test making use of FeatureTypeStyle sortBy to refine query used for styling.
 * 
 * @source $URL$
 */
public class ZOrderTest {
    private static final long TIME = 40000;

    private static final int THRESHOLD = 100;

    SimpleFeatureSource zsquares;

    SimpleFeatureSource zroads;

    ReferencedEnvelope bounds;

    SimpleFeatureSource zbuildings;

    @Before
    public void setUp() throws Exception {
        File property = new File(TestData.getResource(this, "zorder/zsquares.properties").toURI());
        PropertyDataStore ds = new PropertyDataStore(property.getParentFile());
        zsquares = ds.getFeatureSource("zsquares");
        zroads = ds.getFeatureSource("zroads");
        zbuildings = ds.getFeatureSource("zbuildings");
        bounds = zsquares.getBounds();
        bounds.expandBy(0.2, 0.2);

        // System.setProperty("org.geotools.test.interactive", "true");

    }

    @Test
    public void testSingleZAscending() throws Exception {
        runZSquaresTest("zorder/zpolygon.sld", "z", "z-ascending");
    }

    @Test
    public void testSingleZDescending() throws Exception {
        runZSquaresTest("zorder/zpolygon.sld", "z D", "z-descending");
    }

    @Test
    public void testSingleCatDescendingZDescending() throws Exception {
        runZSquaresTest("zorder/zpolygon.sld", "cat D,z D", "cat-desc-z-desc");
    }

    @Test
    public void testSingleCatDescendingZAscending() throws Exception {
        runZSquaresTest("zorder/zpolygon.sld", "cat D,z A", "cat-desc-z-asc");
    }

    @Test
    public void testCrossRoadsNoGroup() throws Exception {
        runRoadsTest("zorder/zroads.sld", "z", null, "roads-no-group");
    }

    @Test
    public void testCrossRoadsGrouped() throws Exception {
        runRoadsTest("zorder/zroads.sld", "z", "theGroup", "roads-group");
    }

    @Test
    public void testCrossRoadsComplexSort() throws Exception {
        runRoadsTest("zorder/zroads.sld", "cat D, name D", "theGroup", "roads-group-complex-sort");
    }

    @Test
    public void testCrossRoadsBuildingsOnZ() throws Exception {
        runRoadsBuildingTest("z", "theGroup", "roads-buildings-group-z");
    }

    private void runZSquaresTest(String styleName, String sortBy, String referenceImageName)
            throws Exception {
        Style style = RendererBaseTest.loadStyle(this, styleName);
        forceSortBy(style, sortBy);

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(zsquares, style));

        runImageComparison(referenceImageName, mc);
    }

    private void runRoadsTest(String styleName, String sortBy, String sortByGroup,
            String referenceImageName) throws Exception {
        Style style = RendererBaseTest.loadStyle(this, styleName);
        forceSortBy(style, sortBy);
        forceSortByGroup(style, sortByGroup);

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(zroads, style));

        runImageComparison(referenceImageName, mc);
    }

    private void runRoadsBuildingTest(String sortBy, String sortByGroup,
            String referenceImageName) throws Exception {
        Style roadsStyle = RendererBaseTest.loadStyle(this, "zorder/zroads.sld");
        forceSortBy(roadsStyle, sortBy);
        forceSortByGroup(roadsStyle, sortByGroup);

        Style buildingsStyle = RendererBaseTest.loadStyle(this, "zorder/zbuildings.sld");
        forceSortBy(buildingsStyle, sortBy);
        forceSortByGroup(buildingsStyle, sortByGroup);

        MapContent mc = new MapContent();
        mc.addLayer(new FeatureLayer(zroads, roadsStyle));
        mc.addLayer(new FeatureLayer(zbuildings, buildingsStyle));

        runImageComparison(referenceImageName, mc);
    }

    private void runImageComparison(String referenceImageName, MapContent mc)
            throws Exception, IOException {
        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mc);
        renderer.setJava2DHints(new RenderingHints(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON));
        renderer.addRenderListener(new RenderListener() {

            @Override
            public void featureRenderer(SimpleFeature feature) {
                // nothing to do
            }

            @Override
            public void errorOccurred(Exception e) {
                fail("No rendering errors expected, but got one: " + e.getMessage());

            }
        });

        BufferedImage image = RendererBaseTest.showRender(referenceImageName, renderer, TIME,
                bounds);
        File reference = new File(
                "./src/test/resources/org/geotools/renderer/lite/test-data/zorder/"
                        + referenceImageName + ".png");
        ImageAssert.assertEquals(reference, image, THRESHOLD);
        mc.dispose();
    }

    private void forceSortBy(Style style, String sortBy) {
        if (sortBy != null) {
            for (FeatureTypeStyle fts : style.featureTypeStyles()) {
                fts.getOptions().put(FeatureTypeStyle.SORT_BY, sortBy);
            }
        }
    }

    private void forceSortByGroup(Style style, String sortByGroup) {
        if (sortByGroup != null) {
            for (FeatureTypeStyle fts : style.featureTypeStyles()) {
                fts.getOptions().put(FeatureTypeStyle.SORT_BY_GROUP, sortByGroup);
            }
        }
    }

}
