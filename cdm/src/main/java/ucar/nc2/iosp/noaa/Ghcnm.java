package ucar.nc2.iosp.noaa;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.TableParser;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.*;

/**
 * cd c:/dev/tds4.2/thredds/cdm/src/main/java
 * protoc --proto_path=. --java_out=. ucar/nc2/iosp/noaa/GhcnmIndex.proto
 *
 * User: caron
 * Date: Dec 8, 2010
 * Time: 12:31:03 PM
 */
public class Ghcnm extends AbstractIOServiceProvider {

  /*
  ftp://ftp.ncdc.noaa.gov/pub/data/ghcn/v3/README

    2.1 METADATA

       The metadata has been carried over from GHCN-Monthly v2.  This would
       include basic geographical station information such as latitude,
       longitude, elevation, station name, etc., and also extended metadata
       information, such as surrounding vegetation, etc.

    2.1.1 METADATA FORMAT

       Variable          Columns      Type
       --------          -------      ----

       ID                 1-11        Integer
       LATITUDE          13-20        Real
       LONGITUDE         22-30        Real
       STNELEV           32-37        Real
       NAME              39-68        Character
       GRELEV            70-73        Integer
       POPCLS            74-74        Character
       POPSIZ            76-79        Integer
       TOPO              80-81        Character
       STVEG             82-83        Character
       STLOC             84-85        Character
       OCNDIS            86-87        Integer
       AIRSTN            88-88        Character
       TOWNDIS           89-90        Integer
       GRVEG             91-106       Character
       POPCSS            107-107      Character

       Variable Definitions:

       ID: 11 digit identifier, digits 1-3=Country Code, digits 4-8 represent
           the WMO stnId if the station is a WMO station.  It is a WMO station if
           digits 9-11="000".

       LATITUDE: latitude of station in decimal degrees

       LONGITUDE: longitude of station in decimal degrees

       STELEV: is the station elevation in meters. -999.0 = missing.

       NAME: station name

       GRELEV: station elevation in meters estimated from gridded digital
               terrain data

       POPCLS: population class
               (U=Urban (>50,000 persons);
               (S=Suburban (>=10,000 and <= 50,000 persons);
               (R=Rural (<10,000 persons)
               City and town boundaries are determined from location of station
               on Operational Navigation Charts with a scale of 1 to 1,000,000.
               For cities > 100,000 persons, population data were provided by
               the United Nations Demographic Yearbook. For smaller cities and
               towns several atlases were uses to determine population.

       POPSIZ: the population of the city or town the station is location in
               (expressed in thousands of persons).

       TOPO: type of topography in the environment surrounding the station,
             (Flat-FL,Hilly-HI,Mountain Top-MT,Mountainous Valley-MV).

       STVEG: type of vegetation in environment of station if station is Rural
              and when it is indicated on the Operational Navigation Chart
              (Desert-DE,Forested-FO,Ice-IC,Marsh-MA).

       STLOC: indicates whether station is near lake or ocean (<= 30 km of
              ocean-CO, adjacent to a lake at least 25 square km-LA).

       OCNDIS: distance to nearest ocean/lake from station (km).

       AIRSTN: airport station indicator (A=station at an airport).

       TOWNDIS: distance from airport to center of associated city or town (km).

       GRVEG: vegetation type at nearest 0.5 deg x 0.5 deg gridded data point of
              vegetation dataset (44 total classifications).

              BOGS, BOG WOODS
              COASTAL EDGES
              COLD IRRIGATED
              COOL CONIFER
              COOL CROPS
              COOL DESERT
              COOL FIELD/WOODS
              COOL FOR./FIELD
              COOL GRASS/SHRUB
              COOL IRRIGATED
              COOL MIXED
              EQ. EVERGREEN
              E. SOUTH. TAIGA
              HEATHS, MOORS
              HIGHLAND SHRUB
              HOT DESERT
              ICE
              LOW SCRUB
              MAIN TAIGA
              MARSH, SWAMP
              MED. GRAZING
              NORTH. TAIGA
              PADDYLANDS
              POLAR DESERT
              SAND DESERT
              SEMIARID WOODS
              SIBERIAN PARKS
              SOUTH. TAIGA
              SUCCULENT THORNS
              TROPICAL DRY FOR
              TROP. MONTANE
              TROP. SAVANNA
              TROP. SEASONAL
              TUNDRA
              WARM CONIFER
              WARM CROPS
              WARM DECIDUOUS
              WARM FIELD WOODS
              WARM FOR./FIELD
              WARM GRASS/SHRUB
              WARM IRRIGATED
              WARM MIXED
              WATER
              WOODED TUNDRA

       POPCSS: population class as determined by Satellite night lights
               (C=Urban, B=Suburban, A=Rural)

    2.2  DATA

         The data within GHCNM v3 beta for the time being consist of monthly
         average temperature, for the 7280 stations contained within GHCNM v2.
         Several new sources have been added to v3 beta, and a new "3 flag"
         format has been introduced, similar to that used within the Global
         Historical Climatology Network-Daily (GHCND).

    2.2.1 DATA FORMAT

          Variable          Columns      Type
          --------          -------      ----

          ID                 1-11        Integer
          YEAR              12-15        Integer
          ELEMENT           16-19        Character
          VALUE1            20-24        Integer
          DMFLAG1           25-25        Character
          QCFLAG1           26-26        Character
          DSFLAG1           27-27        Character
            .                 .             .
            .                 .             .
            .                 .             .
          VALUE12          108-112       Integer
          DMFLAG12         113-113       Character
          QCFLAG12         114-114       Character
          DSFLAG12         115-115       Character

          Variable Definitions:

          ID: 11 digit identifier, digits 1-3=Country Code, digits 4-8 represent
              the WMO stnId if the station is a WMO station.  It is a WMO station if
              digits 9-11="000".

          YEAR: 4 digit year of the station record.

          ELEMENT: element type, currently just "TAVG".

          VALUE: monthly value (MISSING=-9999)

          DMFLAG: data measurement flag, nine possible values:

                  blank = no measurement information applicable
                  a-i = number of days missing in calculation of monthly mean
                        temperature (currently only applies to the 1218 USHCN
                        V2 stations included within GHCNM)

          QCFLAG: quality control flag, seven possibilities within
                  quality controlled unadjusted (qcu) dataset, and 2
                  possibilities within the quality controlled adjusted (qca)
                  dataset.

                  Quality Controlled Unadjusted (QCU) QC Flags:

                  BLANK = no failure of quality control check or could not be
                          evaluated.

                  D = monthly value is part of an annual series of values that
                      are exactly the same (e.g. duplicated) within another
                      year in the station's record.

                  K = monthly value is part of a consecutive run (e.g. streak)
                      of values that are identical.  The streak must be >= 4
                      months of the same value.

                  L = monthly value is isolated in time within the station
                      record, and this is defined by having no immediate non-
                      missing values 18 months on either side of the value.

                  O = monthly value that is >= 5 bi-weight standard deviations
                      from the bi-weight mean.  Bi-weight statistics are
                      calculated from a series of all non-missing values in
                      the station's record for that particular month.

                  S = monthly value has failed spatial consistency check
                      (relative to their respective climatological means to
                       concurrent z-scores at the nearest 20 neighbors located
                       withing 500 km of the target; A temperature fails if
                       (i) its z-score differs from the regional (target and
                       neighbor) mean z-score by at least 3.5 standard
                       deviations and (ii) the target's temperature anomaly
                       differs by at least 2.5 deg C from all concurrent
                       temperature anomalies at the neighbors.

                  T = monthly value has failed temporal consistency check
                      (temperatures whose anomalies differ by more than
                      4 deg C from concurent anomalies at the five nearest
                      neighboring stations whose temperature anomalies are
                      well correlated with the target (correlation > 0.7 for
                      the corresponding calendar monthly).

                  W = monthly value is duplicated from the previous month,
                      based upon regional and spatial criteria and is only
                      applied from the year 2000 to the present.

                  Quality Controlled Adjusted (QCA) QC Flags:

                  M = values with a non-blank quality control flag in the "qcu"
                      dataset are set to missing the adjusted dataset and given
                      an "M" quality control flag.

                  X = pairwise algorithm removed the value because of too many
                      inhomogeneities.


          DSFLAG: data source flag for monthly value, 18 possibilities:

                  C = Monthly Climatic Data of the World (MCDW) QC completed
                      but value is not yet published

                  G = GHCNM v2 station, that was not a v2 station that had multiple
                      time series (for the same element).

                  K = received by the UK Met Office

                  M = Final (Published) Monthly Climatic Data of the World
                     (MCDW)

                  N = Netherlands, KNMI (Royal Netherlans Meteorological
                      Institute)

                  P = CLIMAT (Data transmitted over the GTS, not yet fully
                      processed for the MCDW)

                  U = USHCN v2

                  W = World Weather Records (WWR), 9th series 1991 through 2000

             0 to 9 = For any station originating from GHCNM v2 that had
                      multiple time series for the same element, this flag
                      represents the 12th digit in the ID from GHCNM v2.
                      See section 2.2.2 for additional information.


    2.2.2 STATIONS WITH MULTIPLE TIME SERIES

          The GHCNM v2 contained several thousand stations that had multiple
          time series of monthly mean temperature data.  The 12th digit of
          each data record, indicated the time series number, and thus there
          was a potential maximum of 10 time series (e.g. 0 through 9).  These
          same stations in v3 beta have undergone a merge process, to reduce
          the station time series to one single series, based upon these
          original and at most 10 time series.

          A simple algorithm was applied to perform the merge.  The algorithm
          consisted of first finding the length (based upon number of non
          missing observations) for each of the time series and then
          combining all of the series into one based upon a priority scheme
          that would "write" data to the series for the longest series last.

          Therefore, if station A, had 3 time series of TAVG data, as follows:

          1900 to 1978 (79 years of data) [series 1]
          1950 to 1985 (36 years of data) [series 2]
          1990 to 2007 (18 years of data) [series 3]

          The final series would consist of:

          1900 to 1978 [series 1]
          1979 to 1985 [series 2]
          1990 to 2007 [series 3]

          The original series number in GHCNM v2, is retained in the GHCNM v3
          beta data source flag.

          One caveat to this merge process, is that in the final GHCNM v3 beta
          processing there is still a master level construction process
          performed daily, where the entire dataset is construction according
          to a source order overwrite hiearchy (section 2.3), and it is
          possible that higher order data sources may be interspersed within
          the 3 series listed above.

    2.3 DATA SOURCE HIERARCHY

        The GHCNM v3 beta is reprocessed on a daily basis, which means as a
        part of that reprocessing, the dataset is reconstructed from all
        original sources. The advantage to this process is when source
        datasets are corrected and/or updated the inclusion into GHCNM v3
        beta is seemless.  The following sources (more fully described in
        section 2.2.1) have the following overwrite precedance within the
        daily reprocessing of GHCNM v3 (e.g. source K overwrites source P)

        P,K,G,U,0-9,C,N,M,W
  */


  private static final String RECORD = "data";
  private static final String DIM_NAME = "month";
  private static final String STNID = "stnid";
  private static final String WMO = "wmo";
  private static final String YEAR = "year";
  private static final String VALUE = "value";
  private static final String DMFLAG = "dm";
  private static final String QCFLAG = "qc";
  private static final String DSFLAG = "ds";

  private static final String STNS = "station";
  private static final String LAT = "lat";
  private static final String LON = "lon";
  private static final String STELEV = "elevation";
  private static final String NAME = "name";
  private static final String GRELEV = "grelev";
  private static final String POPCLS = "popClass";
  private static final String POPSIZ = "popSize";
  private static final String TOPO = "topoType";
  private static final String STVEG = "vegType";
  private static final String STLOC = "ocean";
  private static final String OCNDIS = "oceanDist";
  private static final String AIRSTN = "airportId";
  private static final String TOWNDIS = "townDist";
  private static final String GRVEG = "vegType";
  private static final String POPCSS = "popClassFromLights";

  private static final String STN_DATA = "stn_data";

  private NetcdfFile ncfile;
  private RandomAccessFile stnRaf, idxRaf;

  public void close() throws java.io.IOException {
    if (raf != null)
      raf.close();
    if (stnRaf != null)
      stnRaf.close();
    if (idxRaf != null)
      idxRaf.close();

    raf = null;
    stnRaf = null;
    idxRaf = null;
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.ncfile = ncfile;

    String dataFile = raf.getLocation();
    int pos = dataFile.lastIndexOf(".");
    String base = dataFile.substring(0, pos);

    // did the index file get passed in ?
    boolean isIndexFile = isValidFile(raf);
    if (isIndexFile) {
      this.idxRaf = raf;
      // must be in the same directory
      this.raf = new RandomAccessFile(base+".dat", "r");
      this.stnRaf = new RandomAccessFile(base+".inv", "r");

    } else {
      this.raf = raf;
      this.stnRaf = new RandomAccessFile(base+".inv", "r");

      makeIndex(base+".ncsx");

    }

    /*
          ID                 1-11        Integer
          YEAR              12-15        Integer
          ELEMENT           16-19        Character
          VALUE1            20-24        Integer
          DMFLAG1           25-25        Character
          QCFLAG1           26-26        Character
          DSFLAG1           27-27        Character

          ID: 11 digit identifier, digits 1-3=Country Code, digits 4-8 represent
              the WMO stnId if the station is a WMO station.  It is a WMO station if
              digits 9-11="000".
     */

    TableParser parser = new TableParser("11L,15i,19,24i,25,26,27");
    Structure dataSeq = new Sequence(ncfile, null, null, RECORD);
    ncfile.addVariable(null, dataSeq);
    ncfile.addDimension(null, new Dimension(DIM_NAME, 12));
    Variable v;

    makeMember(dataSeq, STNID, DataType.LONG, null, "station stnId", null, null, null);
    makeMember(dataSeq, YEAR, DataType.INT, null, "year of the station record", null, null, null);
    v = makeMember(dataSeq, VALUE, DataType.FLOAT, DIM_NAME, "monthly mean temperature", "Celsius", null, null);
    v.addAttribute(new Attribute(CF.MISSING_VALUE, -9999));
    parser.getField(3).setScale(.01f);
    //v.addAttribute(new Attribute(CF.SCALE_FACTOR, .01f));
    makeMember(dataSeq, DMFLAG, DataType.CHAR, DIM_NAME, "data management flag", null, null, null);
    makeMember(dataSeq, QCFLAG, DataType.CHAR, DIM_NAME, "quality control flag", null, null, null);
    makeMember(dataSeq, DSFLAG, DataType.CHAR, DIM_NAME, "data source flag", null, null, null);

    StructureMembers dataSm = dataSeq.makeStructureMembers();
    dataSm.findMember(STNID).setDataObject(parser.getField(0));
    dataSm.findMember(YEAR).setDataObject(parser.getField(1));
    dataSm.findMember(VALUE).setDataObject(parser.getField(3));
    dataSm.findMember(DMFLAG).setDataObject(parser.getField(4));
    dataSm.findMember(QCFLAG).setDataObject(parser.getField(5));
    dataSm.findMember(DSFLAG).setDataObject(parser.getField(6));
    dataSeq.setSPobject(new Vinfo(this.raf, dataSm));

    /*
       ID                 1-11        Integer
       LATITUDE          13-20        Real
       LONGITUDE         22-30        Real
       STNELEV           32-37        Real
       NAME              39-68        Character
       GRELEV            70-73        Integer
       POPCLS            74-74        Character
       POPSIZ            76-79        Integer
       TOPO              80-81        Character
       STVEG             82-83        Character
       STLOC             84-85        Character
       OCNDIS            86-87        Integer
       AIRSTN            88-88        Character
       TOWNDIS           89-90        Integer
       GRVEG             91-106       Character
       POPCSS            107-107      Character
      */

    parser = new TableParser("11L,20d,30d,37d,68,73i,74,79i,81,83,85,87i,88,90i,106,107");
    Structure stnSeq = new Sequence(ncfile, null, null, STNS);
    ncfile.addVariable(null, stnSeq);

    makeMember(stnSeq, STNID, DataType.LONG, null, "station stnId", null, null, null);
    makeMember(stnSeq, LAT, DataType.FLOAT, null, "latitude", "degrees_north", null, null);
    makeMember(stnSeq, LON, DataType.FLOAT, null, "longitude", "degrees_east", null, null);
    makeMember(stnSeq, STELEV, DataType.FLOAT, null, "elevation", "m", null, null);
    makeMember(stnSeq, NAME, DataType.STRING, null, "station name", null, null, null);
    makeMember(stnSeq, GRELEV, DataType.INT, null, "elevation estimated from gridded digital terrain data", "m", null, null);
    makeMember(stnSeq, POPCLS, DataType.CHAR, null, "population class", null, null, null);
    v = makeMember(stnSeq, POPSIZ, DataType.INT, null, "population of the city or town the station is located in", "thousands of persons", null, null);
    v.addAttribute(new Attribute(CF.MISSING_VALUE, -9));
    makeMember(stnSeq, TOPO, DataType.CHAR, "2", "type of topography in the environment surrounding the station", null, null, null);
    makeMember(stnSeq, STVEG, DataType.CHAR, "2", "type of vegetation in environment of station", null, null, null);
    makeMember(stnSeq, STLOC, DataType.CHAR, "2", "station is near lake or ocean", null, null, null);
    v = makeMember(stnSeq, OCNDIS, DataType.INT, null, "distance to nearest ocean/lake", "km", null, null);
    v.addAttribute(new Attribute(CF.MISSING_VALUE, -9));
    makeMember(stnSeq, AIRSTN, DataType.CHAR, null, "airport station indicator", null, null, null);
    v = makeMember(stnSeq, TOWNDIS, DataType.INT, null, "distance from airport to center of associated city or town", "km", null, null);
    v.addAttribute(new Attribute(CF.MISSING_VALUE, -9));
    makeMember(stnSeq, GRVEG, DataType.STRING, null, "vegetation type at nearest 0.5 deg x 0.5 deg gridded data point of vegetation dataset", null, null, null);
    makeMember(stnSeq, POPCSS, DataType.CHAR, null, "population class as determined by satellite night lights", null, null, null);

    StructureMembers stnSm = stnSeq.makeStructureMembers();
    int count = 0;
    for (StructureMembers.Member m : stnSm.getMembers()) {
      m.setDataObject(parser.getField(count++));
    }
    stnSeq.setSPobject(new Vinfo(stnRaf, stnSm));

    /* Structure nestedSeq = new Sequence(ncfile, null, null, STN_DATA);
    stnSeq.addMemberVariable(nestedSeq);

    makeMember(nestedSeq, STNID, DataType.LONG, null, "station stnId", null, null, null);
    makeMember(nestedSeq, YEAR, DataType.INT, null, "year of the station record", null, null, null);
    v = makeMember(nestedSeq, VALUE, DataType.FLOAT, DIM_NAME, "monthly mean temperature", "Celsius", null, null);
    v.addAttribute(new Attribute(CF.MISSING_VALUE, -9999));
    parser.getField(3).setScale(.01f);
    //v.addAttribute(new Attribute(CF.SCALE_FACTOR, .01f));
    makeMember(nestedSeq, DMFLAG, DataType.CHAR, DIM_NAME, "data management flag", null, null, null);
    makeMember(nestedSeq, QCFLAG, DataType.CHAR, DIM_NAME, "quality control flag", null, null, null);
    makeMember(nestedSeq, DSFLAG, DataType.CHAR, DIM_NAME, "data source flag", null, null, null);   */


    ncfile.addAttribute(null, new Attribute("title", "Version 3 of the GHCN-Monthly dataset of land surface mean temperatures"));
    ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.6"));
    ncfile.addAttribute(null, new Attribute("see", "http://www.ncdc.noaa.gov/ghcnm, ftp://ftp.ncdc.noaa.gov/pub/data/ghcn/v3"));
    ncfile.finish();

  }

  private Variable makeMember(Structure s, String shortName, DataType dataType, String dims, String longName, String units, String cfName,
                                AxisType atype) {

    Variable v = new Variable(ncfile, null, s, shortName, dataType, dims);
    v.addAttribute(new Attribute("long_name", longName));
    if (cfName != null)
      v.addAttribute(new Attribute(CF.STANDARD_NAME, cfName));

    if (units != null)
      v.addAttribute(new Attribute(CF.UNITS, units));

    if (atype != null)
      v.addAttribute(new Attribute(_Coordinate.AxisType, atype.toString()));

    s.addMemberVariable(v);

    return v;
  }


  private class Vinfo {
    RandomAccessFile raf;
    StructureMembers sm;
    int nelems = -1;

    private Vinfo(RandomAccessFile raf, StructureMembers sm) {
      this.sm = sm;
      this.raf = raf;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private static String MAGIC_START = "GhncmIndex";

  private void writeIndex(String indexFilename) throws IOException {
    FileOutputStream fout = new FileOutputStream(indexFilename);
    WritableByteChannel wbc = Channels.newChannel(fout);
    NcStreamWriter ncWriter = new NcStreamWriter(ncfile, indexFilename);
    ncWriter.sendStart(wbc);
    ncWriter.sendHeader(wbc);

    int count = 0, total = 0;
    Sequence seq = (Sequence) ncfile.findVariable(STNS);
    StructureDataIterator iter = seq.getStructureIterator(-1);
    while (iter.hasNext()) {
      count++;
      StructureData sdata = iter.next();
      total += ncWriter.sendData(wbc, sdata);
    }
    System.out.printf("writeIndex stns=%d bytes=%d%n",count, total);
    ncWriter.sendEnd(wbc);
    wbc.close();
  }

  private void makeIndex(String indexFilename) throws IOException {
    HashMap<Long, StationIndex> map = new HashMap<Long, StationIndex>(10000);

    // get map of Stations
    Sequence stnSeq = (Sequence) ncfile.findVariable(STNS);
    Vinfo stnInfo = (Vinfo) stnSeq.getSPobject();
    StructureMembers.Member m = stnInfo.sm.findMember(STNID);
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    int stnCount = 0;

    // read through entire file
    stnInfo.raf.seek(0);
    while (true) {
      long stnPos = stnInfo.raf.getFilePointer();
      String line = stnInfo.raf.readLine();
      if (line == null) break;
      StationIndex s = new StationIndex();
      Long id = (Long) f.parse(line);
      map.put(id, s);
      s.stnId = id;
      s.stnPos = stnPos;
      stnCount++;
    }

    // assumes that the stn data is in order by stnId
    Sequence dataSeq = (Sequence) ncfile.findVariable(RECORD);
    Vinfo dataInfo = (Vinfo) dataSeq.getSPobject();
    m = dataInfo.sm.findMember(STNID);
    f = (TableParser.Field) m.getDataObject();
    StationIndex currStn = null;
    int totalCount = 0;

    // read through entire file
    dataInfo.raf.seek(0);
    while (true) {
      long dataPos = dataInfo.raf.getFilePointer();
      String line = dataInfo.raf.readLine();
      if (line == null) break;

      Long id = (Long) f.parse(line);
      if ((currStn == null) || (currStn.stnId != id)) {
        StationIndex s = map.get(id);
        if (s == null)
          System.out.printf("Cant find %d%n", id);
        else if (s.dataCount != 0)
          System.out.printf("Not in order %d at pos %d %n", id, dataPos);
        else {
          s.dataPos = dataPos;
          totalCount++;
        }
        currStn = s;
      }
      currStn.dataCount++;
    }
    System.out.printf("ok stns=%s data=%d%n", stnCount, totalCount);

    //////////////////////////////
    // write the index file
    FileOutputStream fout = new FileOutputStream(indexFilename);
    long size = 0;

    //// header message
    fout.write(MAGIC_START.getBytes("UTF-8"));
    size += NcStream.writeVInt(fout, stnCount);

    /* byte[] pb = encodeStationListProto( map.values());
    size += NcStream.writeVInt(fout, pb.length);
    size += pb.length;
    fout.write(pb); */

    for (StationIndex s : map.values()) {
      byte[] pb = encodeStationProto(s);
      size += NcStream.writeVInt(fout, pb.length);
      size += pb.length;
      fout.write(pb);
    }   
    fout.close();

    System.out.println(" index size=" + size);
  }

  private class StationIndex {
    long stnId;
    long stnPos; // file pos in inv file
    long dataPos; // file pos of first data line in the data file
    int dataCount; // number of data records
  }

  private byte[] encodeStationProto(StationIndex s)  {
    GhcnmProto.StationIndex.Builder builder = GhcnmProto.StationIndex.newBuilder();
    builder.setStnid(s.stnId);
    builder.setStnPos(s.stnPos);
    builder.setDataPos(s.dataPos);
    builder.setDataCount(s.dataCount);
    ucar.nc2.iosp.noaa.GhcnmProto.StationIndex proto =  builder.build();
    return proto.toByteArray();
  }

  private byte[] encodeStationListProto(Collection<StationIndex> stns)  {
    GhcnmProto.StationIndexList.Builder listBuilder = GhcnmProto.StationIndexList.newBuilder();
    for (StationIndex s : stns) {
      GhcnmProto.StationIndex.Builder builder = GhcnmProto.StationIndex.newBuilder();
      builder.setStnid(s.stnId);
      builder.setStnPos(s.stnPos);
      builder.setDataPos(s.dataPos);
      builder.setDataCount(s.dataCount);
      listBuilder.addList(builder);
    }
    return listBuilder.build().toByteArray();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

   /**
   * Check if this is a valid file for this IOServiceProvider.
   * You must make this method thread safe, ie dont keep any state.
   *
   * @param raf RandomAccessFile
   * @return true if valid.
   * @throws java.io.IOException if read error
   */
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
     raf.seek(0);
     byte[] b = new byte[MAGIC_START.length()];
     raf.read(b);
     String test = new String(b, "UTF-8");
     return test.equals(MAGIC_START);
  }

  /**
   * Get a unique stnId for this file type.
   *
   * @return registered stnId of the file type
   * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  public String getFileTypeId() {
    return "GHCNM";
  }

  /**
   * Get a human-readable description for this file type.
   *
   * @return description of the file type
   * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/FileTypes.html"
   */
  public String getFileTypeDescription() {
    return "GLOBAL HISTORICAL CLIMATOLOGY NETWORK MONTHLY";
  }

  /**
   * Returns an ArraySequence, no subsetting is allowed.
   *
   * @param v2      a top-level Variable
   * @param section the section of data to read.
   *                There must be a Range for each Dimension in the variable, in order.
   *                Note: no nulls allowed. IOSP may not modify.
   * @return ArraySequence
   * @throws IOException
   * @throws InvalidRangeException
   */
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Vinfo vinfo = (Vinfo) v2.getSPobject();
    return new ArraySequence( vinfo.sm, new SeqIter(vinfo), vinfo.nelems);
  }

  /**
   * Get the structure iterator
   *
   * @param s          the Structure
   * @param bufferSize the buffersize
   * @return the data iterator
   * @throws java.io.IOException if problem reading data
   */
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    Vinfo vinfo = (Vinfo) s.getSPobject();
    return new SeqIter(vinfo);
  }

  private class SeqIter implements StructureDataIterator {
    private Vinfo vinfo;
    private long bytesRead;
    private long totalBytes;
    private int recno;
    private StructureData curr;

    SeqIter(Vinfo vinfo) throws IOException {
      this.vinfo = vinfo;
      totalBytes = (int) raf.length();
      vinfo.raf.seek(0);
    }

    @Override
    public StructureDataIterator reset() {
      bytesRead = 0;
      recno = 0;

      try {
        vinfo.raf.seek(0);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @Override
    public boolean hasNext() throws IOException {
      boolean more = (bytesRead < totalBytes); // && (recno < 10);
      if (!more) {
        vinfo.nelems = recno;
        //System.out.printf("nelems=%d%n", recno);
        return false;
      }
      curr = reallyNext();
      more = (curr != null);
      if (!more) {
        vinfo.nelems = recno;
        //System.out.printf("nelems=%d%n", recno);
        return false;
      }
      return more;
    }

    @Override
    public StructureData next() throws IOException {
      return curr;
    }

    private StructureData reallyNext() throws IOException {
      String line;
      while (true) {
        line = vinfo.raf.readLine();
        if (line == null) return null;
        if (line.startsWith("#")) continue;
        if (line.trim().length() == 0) continue;
        break;
      }
      //System.out.printf("%s%n", line);
      bytesRead = vinfo.raf.getFilePointer();
      recno++;
      return new StructureDataAscii(vinfo.sm, line);
    }

    @Override
    public void setBufferSize(int bytes) {
    }

    @Override
    public int getCurrentRecno() {
      return recno - 1;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  static public NetcdfFile open(String filename) throws IOException {
    Ghcnm iosp = new Ghcnm();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    MyNetcdfFile ncfile = new MyNetcdfFile(iosp);
    iosp.open(raf, ncfile, null);
    return ncfile;
  }

  static private class MyNetcdfFile extends NetcdfFile {
    MyNetcdfFile(Ghcnm iosp) {
      this.spi = iosp;
    }
  }

  static public void doOne(String filename, Set<Integer> stns, boolean wantDups) throws IOException {
    System.out.printf("%s%n",filename);
    int count = 0;
    int countDups = 0;
    NetcdfFile ncfile = open(filename);
    Sequence seq = (Sequence) ncfile.findVariable(STNS);
    StructureDataIterator iter = seq.getStructureIterator(-1);
    while (iter.hasNext()) {
      count++;
      StructureData sdata = iter.next();
      StructureMembers.Member m = sdata.findMember(STNID);
      int stnid = sdata.getScalarInt(m);
      if (stns.contains(stnid)) {
        countDups++;
        if (!wantDups) System.out.printf("  dup %d%n",stnid);
      } else {
        stns.add(stnid);
        if (wantDups) System.out.printf("  dup %d%n",stnid);
      }
    }
    System.out.printf(" counts=%d dups=%d%n",count,countDups);
  }


  static public void main(String args[]) throws IOException {
    Set<Integer> stns = new HashSet<Integer>(10 * 1000);
    doOne("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qae.inv", stns, false);
    doOne("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qca.inv", stns, true);
    doOne("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qcu.inv", stns, true);
  }
}

/*

  static public void main(String args[]) throws IOException {
    //InputStream is = cl.getResourceAsStream("resources/nj22/tables/nexrad.tbl");
    InputStream is = new FileInputStream("C:/data/ghcnm/ghcnm.v3.0.0-beta1.20101207.qae.inv");

    List<TableParser.Record> recs = TableParser.readTable(is, "11,20d,30d,37d,68,73i,74,79i,81,83,85,87i,88,90i,106,107", 10);
    Formatter f = new Formatter(System.out);
    //f.format("CNTRY WMO ID      YEAR  ELEM  VAL DM QC DS%n");
    for (TableParser.Record record : recs) {
      record.toString(f);
    }
  }
 */