
# Penalties

The backend for the penalties services such as `penalties-frontend` and `penalties-appeals-frontend`. It handles the following:

- Retrieving penalties and compliance data
- The retrieval and submission of appeals
- Providing other teams with penalties summary data 

## Running

This services requires MongoDB to be running.

The service manager configuration name for this service is `PENALTIES`. 
But it can be started with`sm --start PENALTIES_ALL` along with dependent services such as `penalties-stub` and more. 
It can also be started specifically with `sm --start PENALTIES`. 

To run local changes you will have to stop the service in service manager using `sm --stop PENALTIES` then run the script mentioned below.

This application runs on port 9182 .

To start the service use the `./run.sh` script.

## Testing

This service can be tested with SBT via `sbt test it:test`

To run coverage and scalastyle, please run: `sbt clean scalastyle coverage test it:test coverageReport`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
