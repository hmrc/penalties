
# Penalties

The backend for the penalties services. It handles the following:

- Retrieving penalties and compliance data
- The retrieval and submission of appeals
- Providing other teams with penalties summary data 

## Running

The service manager configuration name for this service is `PENALTIES`. 
But it can be started with`sm --start PENALTIES_ALL` along with dependent services such as `penalties-stub` and more.

This application runs on port 9182 .

To start the service use the `./run.sh` script.

## Testing

This service can be tested with SBT via `sbt test it:test`

To run coverage and scalastyle, please run: `sbt clean scalastyle coverage test it:test coverageReport`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
