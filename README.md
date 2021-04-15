# Philita OTP Fork 
_Work on only branch 2.0-rc_

## 2.0-rc
We are working on the most recent stable build of the OTP. 

Update this README with a list of any changes that have been made. 

## Changes

- Added tripData class to org.opentripplanner.transit.raptor.transit.tripData;
- Added trip occupancy code to DefaultCostCalculator class. See OccupancyV3 for more info.

28/03
- Removed tripData class from org.opentripplanner.transit.raptor.transit.tripData
- Packaged tripData class as a jar, deployed with mvn to a newly created repository at //src/repo-PHilita
- Added a normal dependency to the jar in the pom.xml
- Currently not working - need to change the links and such