package org.orbisgis.orbisprocess.geoclimate.geoindicators

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.orbisgis.orbisdata.datamanager.jdbc.h2gis.H2GIS.open

class SpatialUnitsTests {

    private static def h2GIS
    private static def randomDbName() {"${SpatialUnitsTests.simpleName}_${UUID.randomUUID().toString().replaceAll"-", "_"}"}

    @BeforeAll
    static void beforeAll(){
        h2GIS = open"./target/${randomDbName()};AUTO_SERVER=TRUE"
    }

    @BeforeEach
    void beforeEach(){
        h2GIS.executeScript(getClass().getResourceAsStream("data_for_tests.sql"))
    }

    @Test
    void createRSUTest() {
        h2GIS """
                DROP TABLE IF EXISTS roads_rsu;
                CREATE TABLE roads_rsu AS SELECT * FROM road_test WHERE id_road <5
        """
        def rsu = Geoindicators.SpatialUnits.createRSU()
        assert rsu([
                inputTableName  : "roads_rsu",
                prefixName      : "rsu",
                datasource      : h2GIS])
        def outputTable = rsu.results.outputTableName
        def countRows = h2GIS.firstRow "select count(*) as numberOfRows from $outputTable"
        assert 9 == countRows.numberOfRows
    }

    @Test
    void prepareGeometriesForRSUTest() {
        h2GIS.load(SpatialUnitsTests.getResource("road_test.geojson"), true)
        h2GIS.load(SpatialUnitsTests.getResource("rail_test.geojson"), true)
        h2GIS.load(SpatialUnitsTests.getResource("veget_test.geojson"), true)
        h2GIS.load(SpatialUnitsTests.getResource("hydro_test.geojson"), true)
        h2GIS.load(SpatialUnitsTests.getResource("zone_test.geojson"),true)

        def prepareData = Geoindicators.SpatialUnits.prepareRSUData()
        assert prepareData([
                zoneTable           : 'zone_test',
                roadTable           : 'road_test',
                railTable           : 'rail_test',
                vegetationTable     : 'veget_test',
                hydrographicTable   :'hydro_test',
                surface_vegetation  : null,
                surface_hydro       : null,
                prefixName          : "block",
                datasource          : h2GIS])

        def outputTableGeoms = prepareData.results.outputTableName

        assert h2GIS."$outputTableGeoms"

        def rsu = Geoindicators.SpatialUnits.createRSU()
        assert rsu([inputTableName: outputTableGeoms, prefixName: "rsu", datasource: h2GIS])
        def outputTable = rsu.results.outputTableName
        assert h2GIS.save(outputTable,'./target/rsu.shp')
        def countRows = h2GIS.firstRow "select count(*) as numberOfRows from $outputTable"

        assert 246 == countRows.numberOfRows
    }


    @Test
    void createBlocksTest() {
        h2GIS """
                DROP TABLE IF EXISTS build_tempo; 
                CREATE TABLE build_tempo AS SELECT * FROM building_test WHERE id_build <27
        """
        def blockP = Geoindicators.SpatialUnits.createBlocks()
        assert blockP([inputTableName: "build_tempo",distance:0.01,prefixName: "block", datasource: h2GIS])
        def outputTable = blockP.results.outputTableName
        def countRows = h2GIS.firstRow "select count(*) as numberOfRows from $outputTable"
        assert 12 == countRows.numberOfRows
    }

    @Test
    void spatialJoinTest() {
        h2GIS """
                DROP TABLE IF EXISTS build_tempo; 
                CREATE TABLE build_tempo AS 
                    SELECT id_build, the_geom FROM building_test 
                    WHERE id_build < 9 OR id_build > 28 AND id_build < 30
        """
        def pRsu =  Geoindicators.SpatialUnits.spatialJoin()
        assert pRsu.execute([
                inputTable1     : "build_tempo",
                inputTable2     : "rsu_test",
                idColumnTab1    : "id_build",
                idColumnTab2    : "id_rsu",
                pointOnSurface  : false,
                nbRelations     : 1,
                prefixName      : "test",
                datasource      : h2GIS])
        h2GIS.eachRow("SELECT * FROM ${pRsu.results.outputTableName}"){
            row ->
                def expected = h2GIS.firstRow("SELECT ${pRsu.results.idColumnTab2} FROM rsu_build_corr WHERE id_build = ${row.id_build}".toString())
                assert row[pRsu.results.idColumnTab2] == expected[pRsu.results.idColumnTab2]
        }
        def pBlock =  Geoindicators.SpatialUnits.spatialJoin()
        assert pBlock([
                inputTable1     : "build_tempo",
                inputTable2     : "block_test",
                idColumnTab1    : "id_build",
                idColumnTab2    : "id_block",
                pointOnSurface  : false,
                nbRelations     : 1,
                prefixName      : "test",
                datasource      : h2GIS])

        h2GIS.eachRow("SELECT * FROM ${pBlock.results.outputTableName}".toString()){
            row ->
                def expected = h2GIS.firstRow "SELECT ${pBlock.results.idColumnTab2} FROM block_build_corr WHERE id_build = ${row.id_build}".toString()
                assert row[pBlock.results.idColumnTab2] == expected[pBlock.results.idColumnTab2]
        }
    }

    @Test
    void spatialJoinTest2() {
        h2GIS """
                DROP TABLE IF EXISTS tab1, tab2;
                CREATE TABLE tab1(id1 int, the_geom geometry);
                CREATE TABLE tab2(id2 int, the_geom geometry);
                INSERT INTO tab1 VALUES (1, 'POLYGON((0 0, 10 0, 10 20, 0 20, 0 0))'),
                                        (2, 'POLYGON((10 5, 20 5, 20 20, 10 20, 10 5))'),
                                        (3, 'POLYGON((10 -10, 25 -10, 25 5, 10 5, 10 -10))');
                INSERT INTO tab2 VALUES (1, 'POLYGON((0 0, 20 0, 20 20, 0 20, 0 0))'),
                                        (2, 'POLYGON((20 0, 30 0, 30 20, 20 20, 20 0))');
        """
        def pNbRelationsAll =  Geoindicators.SpatialUnits.spatialJoin()
        assert pNbRelationsAll.execute([
                inputTable1     : "tab1",
                inputTable2     : "tab2",
                idColumnTab1    : "id1",
                idColumnTab2    : "id2",
                pointOnSurface  : false,
                nbRelations     : null,
                prefixName      : "test",
                datasource      : h2GIS])
        def area1_1 = h2GIS.firstRow("SELECT AREA FROM ${pNbRelationsAll.results.outputTableName} WHERE ID1 = 1 AND ID2=1")
        def area2_1 = h2GIS.firstRow("SELECT AREA FROM ${pNbRelationsAll.results.outputTableName} WHERE ID1 = 2 AND ID2=1")
        def area3_1 = h2GIS.firstRow("SELECT AREA FROM ${pNbRelationsAll.results.outputTableName} WHERE ID1 = 3 AND ID2=1")
        def area3_2 = h2GIS.firstRow("SELECT AREA FROM ${pNbRelationsAll.results.outputTableName} WHERE ID1 = 3 AND ID2=2")

        assert area1_1.AREA == 200
        assert area2_1.AREA == 150
        assert area3_1.AREA == 50
        assert area3_2.AREA == 25

        def pPointOnSurface =  Geoindicators.SpatialUnits.spatialJoin()
        assert pPointOnSurface.execute([
                inputTable1     : "tab1",
                inputTable2     : "tab2",
                idColumnTab1    : "id1",
                idColumnTab2    : "id2",
                pointOnSurface  : true,
                nbRelations     : null,
                prefixName      : "test",
                datasource      : h2GIS])
        assert h2GIS.getTable(pPointOnSurface.results.outputTableName).getRowCount() == 2
    }

    @Test
    void prepareGeometriesForRSUWithFilterTest() {
        h2GIS.load(SpatialUnitsTests.class.class.getResource("road_test.geojson"), true)
        h2GIS.load(SpatialUnitsTests.class.class.getResource("rail_test.geojson"), true)
        h2GIS.load(SpatialUnitsTests.class.class.getResource("veget_test.geojson"), true)
        h2GIS.load(SpatialUnitsTests.class.class.getResource("hydro_test.geojson"), true)
        h2GIS.load(SpatialUnitsTests.class.class.getResource("zone_test.geojson"),true)

        def  prepareData = Geoindicators.SpatialUnits.prepareRSUData()

        assert prepareData([
                zoneTable               : 'zone_test',
                roadTable               : 'road_test',
                railTable               : 'rail_test',
                vegetationTable         : 'veget_test',
                hydrographicTable       : 'hydro_test',
                surface_vegetation      : null,
                surface_hydro           : null,
                prefixName              : "block",
                datasource              : h2GIS])


        def outputTableGeoms = prepareData.results.outputTableName

        assert h2GIS."$outputTableGeoms"
        def rsu = Geoindicators.SpatialUnits.createRSU()
        assert rsu.execute([inputTableName: outputTableGeoms, prefixName: "rsu", datasource: h2GIS])
        def outputTable = rsu.results.outputTableName
        assert h2GIS.save(outputTable,'./target/rsu.shp')
        def countRows = h2GIS.firstRow "select count(*) as numberOfRows from $outputTable"

        assert 246 == countRows.numberOfRows
    }

}