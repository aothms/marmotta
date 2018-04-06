# Apache Marmotta

This repository contains the source code for a modified version of [Apache Marmotta](https://marmotta.apache.org/)

Modifications are minor and prototypical and enable storage of polyhedra WKT and making use of PostGIS/SFCGAL 3d operators.

## About the example dataset

The provided dataset in the ./example/ folder is a subset of the 3d office building model provided by US ACE and buildingSMART alliance [1] converted to RDF using [2]. An example query for spaces without air inlets is given below:

    PREFIX ifcowl: <http://www.buildingsmart-tech.org/ifcOWL/IFC2X3_TC1#>
    PREFIX geo: <http://www.opengis.net/ont/geosparql#>
    PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
    PREFIX express:  <http://purl.org/voc/express#>
    
    SELECT ?name WHERE {
      ?s a ifcowl:IfcSpace .
      ?s geo:hasGeometry/geo:asWKT ?sg .
      ?s ifcowl:name_IfcRoot/express:hasString ?name .
      
      FILTER NOT EXISTS {
        ?t a ifcowl:IfcFlowTerminal .
        ?t geo:hasGeometry/geo:asWKT ?tg .
        ?rel ifcowl:relatedObjects_IfcRelDefines ?t .
        ?rel ifcowl:relatingType_IfcRelDefinesByType ?type .
        ?type a ifcowl:IfcAirTerminalType .
        
        FILTER(geof:distance(?tg, ?sg) < 0.01)
      }
    }
    
Which translates to the following SQL query. Take note of the the CASE statement to switch between a 2d and 3d distance implementation.

    SELECT P5.object AS V5
    
    FROM triples P1
    CROSS JOIN triples P2
    CROSS JOIN triples P3
    INNER JOIN nodes AS P3_object_V3 ON P3.object = P3_object_V3.id
    CROSS JOIN triples P4
    CROSS JOIN triples P5
    
    WHERE P1.deleted = false
      AND P1.predicate = 982184394049863680
      AND P1.object = 982184507212185602
      AND P2.deleted = false
      AND P1.subject = P2.subject
      AND P2.predicate = 982184530184388611
      AND P3.deleted = false
      AND P2.object = P3.subject
      AND P3.predicate = 982184530196971522
      AND P4.deleted = false
      AND P1.subject = P4.subject
      AND P4.predicate = 982184434302599170
      AND P5.deleted = false
      AND P4.object = P5.subject
      AND P5.predicate = 982184433149165571
      AND NOT ( EXISTS ( 
        
        SELECT *
        
        FROM triples _P1
        CROSS JOIN triples _P2
        CROSS JOIN triples _P3
        INNER JOIN nodes AS _P3_object_V3 ON _P3.object = _P3_object_V3.id
        CROSS JOIN triples _P4
        CROSS JOIN triples _P5
        CROSS JOIN triples _P6
        
        WHERE _P1.deleted = false
          AND _P1.predicate = 982184394049863680
          AND _P1.object = 982184434369708034
          AND _P2.deleted = false
          AND _P1.subject = _P2.subject
          AND _P2.predicate = 982184530184388611
          AND _P3.deleted = false
          AND _P2.object = _P3.subject
          AND _P3.predicate = 982184530196971522
          AND _P4.deleted = false
          AND _P1.subject = _P4.object
          AND _P4.predicate = 982184468532314120
          AND _P5.deleted = false
          AND _P4.subject = _P5.subject
          AND _P5.predicate = 982184468964327424
          AND _P6.deleted = false
          AND _P5.object = _P6.subject
          AND _P6.predicate = 982184394049863680
          AND _P6.object = 982184468976910338
          AND _P3_object_V3.gvalue IS NOT NULL
          AND P3_object_V3.gvalue IS NOT NULL
          AND ( CASE
            WHEN (ST_CoordDim(_P3_object_V3.gvalue) = 3 and ST_CoordDim(P3_object_V3.gvalue) = 3)
            THEN (ST_3DDistance(_P3_object_V3.gvalue, P3_object_V3.gvalue))
            ELSE (ST_Distance(_P3_object_V3.gvalue, P3_object_V3.gvalue))
          END ) < 0.01
          
    ) )

A graphical depiction of the query resulted

![3D GeoSPARQL result render](https://raw.githubusercontent.com/aothms/marmotta/MARMOTTA-584/example/geosparql-result-render.png)

A similar approach has been implemented for geof:sfIntersects. A more elaborate (but not performant) variant that appeared most reliable for (volumetric!) solid intersections.

    CASE
        WHEN (
            ST_CoordDim({0}) = 3 and
            ST_CoordDim({1}) = 3 and
            ST_NumPatches({0}) is not null and
            ST_NumPatches({1}) is not null
        )
        THEN (
            CASE
                WHEN {0} &&& {1}
                THEN (
                    ( ST_Volume(ST_3Dunion(
                        st_makesolid({0}),
                        st_makesolid({1})
                    )) + 0.000001 ) <
                    ST_Volume(st_makesolid({0})) +
                    ST_Volume(st_makesolid({1}))
                )
                ELSE FALSE
            END
        )
        ELSE ST_Intersects({0}, {1})
    END


Users are advised to use the Dockerfile.

Further reading:

Pauwels P., Krijnen T., Terkaj W., Beetz J. (2017) Enhancing the ifcOWL ontology with an alternative representation for geometric data, Automation in Construction, Volume 80, Pages 77-94, https://doi.org/10.1016/j.autcon.2017.03.001.

[1] https://www.nibs.org/?page=bsa_commonbimfiles#project2
[2] https://github.com/IDLabResearch/IFC-to-RDF-converter

* ... official readme continues below ... *

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.marmotta/marmotta-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.apache.marmotta/marmotta-core/)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

## Building the Source Distribution

Apache Marmotta uses Maven to build, test, and install the software. A basic
build requires downloading and installing Maven and then running:

    mvn clean install

This will compile, package and test all Apache Marmotta modules and install
it in your local Maven repository. In case you want to build your own
projects based on some of the libraries provided by Apache Marmotta, this
usually suffices.

The default loglevel for most unit and integration tests executed during the
build is INFO. To change the loglevel for either more or less output, you
can pass the loglevel as system property:

    mvn clean install -Droot-level=TRACE|DEBUG|INFO|WARN|ERROR

Note that some of the integration tests start up parts of the Marmotta
platform during execution. The log level for these tests cannot be
changed, as Marmotta is taking over the log configuration in these
cases.

## Building, Running and Deploying the Wep Application

Apache Marmotta also includes a default configuration for building a Java
Web Application that can be deployed in any Java Application Server. To
build the web application, first run

    mvn clean install

in the project root. Then change to the launchers/marmotta-webapp directory
and run

    mvn package

This will create a marmotta.war file in the target/ directory. You can
deploy this archive to any Java Application Server by copying it into
its deployment directory; [more details](http://marmotta.apache.org/installation.html).

Alternatively, you can directly startup the Apache Marmotta Web Application
from Maven with a default configuration suitable for development. To try this
out, run

    mvn tomcat7:run

wait until the system is started up and point your browser to `http://localhost:8080`

When developing it is sometimes useful to always start with a clean confi-
guration of the system. Therefore, you can also start up the web application
as follows:

    mvn clean tomcat7:run -Pcleanall

This command will remove any existing configuration directory before startup.

## Building the Standalone Installer

The build environment also offers to automatically build an installer package
that guides users through the installation with an easy-to-use installation
wizard. The installer is based on izPack and dynamically assembled when
building the package. To build the installer, first run

    mvn clean install

in the project root. Then change to the launchers/marmotta-installer directory
and run

    mvn package -Pinstaller

The build process will automatically create an appropriate installer confi-
guration from the Maven dependencies through the Apache Marmotta refpack
build plugin.

The installer can then be tried out by running

    java -jar target/marmotta-installer-x.x.x.jar


## Building a Docker image

Marmotta also comes with support for creating a Docker images that you can use for development 
or testing:

1. Locate at the root of the source repository
2. Build image: `docker build -t marmotta .`
3. Run the container: `docker run -p 8080:8080 marmotta`
4. Access Marmotta at [localhost:8080/marmotta](http://localhost:8080/marmotta) (IP address may 
   be different, see information bellow).

An official images is [available from Docker Hub](https://hub.docker.com/r/apache/marmotta/) as 
an automated build, so you just need to pull it from there to replace the second step above: 

    docker pull apache/marmotta


## Building with a Clean Repository

Sometimes it is useful to check if the build runs properly on a clean local
repository, i.e. simulate what happens if a user downloads the source and
runs the build. This can be achieved by running Maven as follows:

    mvn clean install -Dmaven.repo.local=/tmp/testrepo

The command changes the local repository location from ~/.m2 to the
directory passed as argument


## Simulating a Release

To test the release build without actually deploying the software, we have
created a profile that will deploy to the local file system. You can
simulate the release by running

    mvn clean deploy -Pdist-local,marmotta-release,installer

Please keep in mind that building a release involves creating digital
signatures, so you will need a GPG key and a proper GPG configuration to run
this task.

Read more about [our release process](https://wiki.apache.org/marmotta/ReleaseProcess).

