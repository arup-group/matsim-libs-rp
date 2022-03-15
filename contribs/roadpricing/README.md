
# Road Pricing

This package provides functionality to simulate different road-pricing scenarios in MATSim. 

It provides support for different toll schemes, for example distance tolls, cordon tolls and area tolls. 
The toll schemes are described in special XML files (see below).

All supported toll schemes can be limited to a part of the network and can be time-dependent (that means that the amount
agents have to pay for the toll can differ during the simulated day).

The specified toll amount should be in respect to the scoring function used. Typically, the scoring function contains a
parameter that defines the marginal utility of money; that parameter is used to convert toll amounts into utilities.

For more informations, please consult the
[javadoc.](http://ci.matsim.org:8080/job/MATSim_contrib_M2/org.matsim.contrib$roadpricing/javadoc/?)

# Arup CML Fork

We forked `matsim-libs` initially to make some small changes to this road pricing module, as discussed in this
[issue on MATSim central](https://github.com/matsim-org/matsim-libs/issues/1898). Given that we are currently only
making changes in this module, I have modified the `contribs/roadpricing/pom.xml` so that building this module now
produces a Maven artefact in `contribs/roadpricing/target/` called `arup-roadpricing-14.0-SNAPSHOT.jar`, to
differentiate it from the version from MATSim central. If all you are doing is making changes to this road pricing
module and nothing else, you can build the jar file independently of the rest of the MATSim project - this will be
*much* quicker than running a full MATSim build:

```bash
cd contribs/roadpricing/
mvn clean package

...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 39.725 s
[INFO] Finished at: 2022-03-11T13:01:32Z
[INFO] Final Memory: 31M/117M
[INFO] ------------------------------------------------------------------------

ls -talh target/

total 208
drwxr-xr-x   9 mickyfitz  staff   288B 11 Mar 13:02 ..
-rw-r--r--   1 mickyfitz  staff    42K 11 Mar 13:01 arup-roadpricing-14.0-SNAPSHOT-sources.jar
drwxr-xr-x  11 mickyfitz  staff   352B 11 Mar 13:01 .
-rw-r--r--   1 mickyfitz  staff    57K 11 Mar 13:01 arup-roadpricing-14.0-SNAPSHOT.jar
drwxr-xr-x   3 mickyfitz  staff    96B 11 Mar 13:01 maven-archiver
drwxr-xr-x  18 mickyfitz  staff   576B 11 Mar 13:01 surefire-reports
drwxr-xr-x   3 mickyfitz  staff    96B 11 Mar 13:00 test-classes
drwxr-xr-x   3 mickyfitz  staff    96B 11 Mar 13:00 generated-test-sources
drwxr-xr-x   3 mickyfitz  staff    96B 11 Mar 13:00 generated-sources
drwxr-xr-x   3 mickyfitz  staff    96B 11 Mar 13:00 maven-status
drwxr-xr-x   4 mickyfitz  staff   128B 11 Mar 13:00 classes
```
 