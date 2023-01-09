 
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


## Endpoints

The payload must be pre-existing in Mongo before attempting a retrieval.

#### `GET        /penalties/vat/penalties/summary/:VRN`
Retrieve penalties summary information belonging to a VRN.

Example VRN: `123456789`

Example payload:
```
{
  "noOfPoints": 3,
  "noOfEstimatedPenalties": 2,
  "noOfCrystalisedPenalties": 1,
  "estimatedPenaltyAmount": 123.45,
  "crystalisedPenaltyAmountDue": 54.32,
  "hasAnyPenaltyData": true
}
```

| Scenario              | Status |
|-----------------------|--------|
| Successful retrieval  | 200    |
| Invalid VRN format    | 400    |
| No data found for VRN | 404    |

#### `GET        /penalties/etmp/penalties/:enrolmentKey`
Get data for penalties belonging to an enrolment key. 

Example enrolmentKey format: `HMRC-MTD-VAT~VRN~123456789`
 
| Scenario              | Status |
|-----------------------|--------|
| Successful retrieval  | 200    |
| Invalid VRN format    | 400    |
| No data found for VRN | 404    |

### Appeals:
#### `GET        /penalties/appeals-data/late-submissions`

Get Late Submission Penalty data for an appeal.

The following query parameters should be specified:

| Parameter        | Type   | Mandatory |
|------------------|--------|-----------|
| `penaltyId`      | String | Yes       |
| `enrolmentKey`   | String | Yes       |
| `useNewApiModel` | String | Yes       |

URL format - `/penalties/appeals-data/late-submissions?{penaltyId}=[idvalue]&{enrolementKey}=[key]`

Example URL - `/penalties/appeals-data/late-submissions?penaltyId=1234567890&enrolementKey=HMRC-MTD-VAT~VRN~123456789`

| Scenario                    | Status |
|-----------------------------|--------|
| Successful retrieval        | 200    |
| No data found for penaltyId | 404    |
| Internal server error       | 500    |

#### `GET        /penalties/appeals-data/late-payments`

Get Late Payment Penalty data for an appeal.

The following query parameters should be specified:

| Parameter        | Type    | Mandatory |
|------------------|---------|-----------|
| `penaltyId`      | String  | Yes       |
| `enrolmentKey`   | String  | Yes       |       
| `isAdditional`   | Boolean | Yes       |     
| `useNewApiModel` | Boolean | Yes       |    

URL format - `/appeals-data/late-submissions?{penaltyId}=[idvalue]&{enrolementKey}=[value]&{isAdditional}=[booleanValue]`

Example URL - `/appeals-data/late-submissions?penaltyId=1234567890&enrolementKey=HMRC-MTD-VAT~VRN~123456789&isAdditional=false`

| Scenario                    | Status |
|-----------------------------|--------|
| Successful retrieval        | 200    |
| No data found for penaltyId | 404    |
| Internal server error       | 500    |

#### `GET        /penalties/appeals-data/reasonable-excuses`

Get list of reasonable excuses used to make an appeal.

| Scenario             | Status |
|----------------------|--------|
| Successful retrieval | 200    |


#### `POST       /penalties/appeals/submit-appeal`

Submit an appeal for a penalty.

The following query parameters should be specified:

| Parameter       | Type    | Mandatory |
|-----------------|---------|-----------|
| `enrolmentKey`  | String  | Yes       |
| `isLPP`         | Boolean | Yes       |       
| `penaltyNumber` | String  | Yes       |     
| `correlationId` | String  | Yes       |     

URL format - `/penalties/appeals/submit-appeal?{enrolmentKey}=[keyValue]&{isLPP}=[boolValue]&{penaltyNumber}=[penaltyId]&{correlationId}=[value]`

Example URL - `/penalties/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~224060020&isLPP=false&penaltyNumber=123456786&correlationId=a8010aef-9253-45a8-b8ac-c843dc2d3318`

Example payload:
```
{
    "sourceSystem": "MDTP",
    "taxRegime": "VAT",
    "customerReferenceNo": "123456789",
    "dateOfAppeal": "2020-01-01T00:00:00",
    "isLPP": true,
    "appealSubmittedBy": "agent",
    "agentReferenceNo": "AGENT1",
    "appealInformation": {
            "reasonableExcuse": "crime",
            "honestyDeclaration": true,
            "startDateOfEvent": "2021-04-23T00:00",
            "reportedIssueToPolice": true,
            "lateAppeal": true,
            "lateAppealReason": "Reason",
            "isClientResponsibleForSubmission": false,
            "isClientResponsibleForLateSubmission": true
    }
}
```

| Scenario                                         | Status |
|--------------------------------------------------|--------|
| Successful post                                  | 200    |
| Invalid body received / Failed to parse to model | 400    |
| Internal server error                            | 500    |

### Compliance
#### `GET        /penalties/compliance/des/compliance-data`
Get compliance data of penalty for VRN in a period.

The following query parameters should be specified:

| Parameter  | Type              | Mandatory |
|------------|-------------------|-----------|
| `VRN`      | String            | Yes       |
| `dateFrom` | Date (YYYY-MM-DD) | Yes       |       
| `dateTo`   | Date (YYYY-MM-DD) | Yes       |     

URL format - `/penalties/compliance/des/compliance-data?{VRN}=[value]&{fromDate}=[date]&{toDate}=[date]`

Example URL - `/penalties/compliance/des/compliance-data?VRN=1234567890&fromDate=2020-01-01&toDate=2020-03-31`

| Scenario                 | Status |
|--------------------------|--------|
| Successful retrieval     | 200    |
| Failed to parse to model | 400    |
| No data found for VRN    | 404    |
| Internal server error    | 500    |

### Get Financial Details (API 1811)
#### `GET        /penalties/penalty/financial-data/VRN/:VRN/VATC`

Gets the financial details for the specified VRN.

The following query parameters can be specified:

| Parameter                    | Type              | Mandatory |
|------------------------------|-------------------|-----------|
| `searchType`                 | String            | No        |
| `searchItem`                 | String            | No        |
| `dateType`                   | String            | No        |
| `dateFrom`                   | Date (YYYY-MM-DD) | No        |
| `dateTo`                     | Date (YYYY-MM-DD) | No        |
| `includeClearedItems`        | Boolean           | No        |
| `includeStatisticalItems`    | Boolean           | No        |
| `includePaymentOnAccount`    | Boolean           | No        |
| `addRegimeTotalisation`      | Boolean           | No        |
| `addLockInformation`         | Boolean           | No        |
| `addPenaltyDetails`          | Boolean           | No        |
| `addPostedInterestDetails`   | Boolean           | No        |
| `addAccruingInterestDetails` | Boolean           | No        |

Example URL - `/penalties/penalty/financial-data/VRN/:VRN/VATC?searchType=CHGREF&searchItem=XC00178236592&dateType=BILLING&dateFrom=2020-10-03&dateTo=2021-07-12&includeClearedItems=false&includeStatistical=true&includePaymentOnAccount=true&addRegimeTotalisation=false&addLockInformation=true&addPenaltyDetails=true&addPostedInterestDetails=true&addAccruingInterestDetails=true`

| Scenario                                                                                                                                        | Status |
|-------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| Successful retrieval                                                                                                                            | 200    |
| Bad request due to one or more invalid parameters, see EIS spec for details of different errors (error message body is passed through from EIS) | 400    |
| No data found for VRN                                                                                                                           | 404    |
| Internal server error                                                                                                                           | 500    |
| Dependent systems are not available                                                                                                             | 503    |

The data returned is outlined in v2.3.0 of the GetFinancialDetails API specification.

### Get Penalty Details (API 1812)
### `GET        /penalties/penalty-details/VAT/VRN/:VRN`

Gets the penalty details for specified VRN.

The following query parameter can be specified

| Parameter   | Type   | Mandatory | Comments                                            |
|-------------|--------|-----------|-----------------------------------------------------|
| `dateLimit` | String | No*       | This will expected to be 24 months unless specified |

Example URL - `/penalties/penalty-details/VAT/VRN/:VRN?dateLimit=09`

| Scenario                                                                                                                                        | Status |
|-------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| Successful retrieval                                                                                                                            | 200    |
| Bad request due to one or more invalid parameters, see EIS spec for details of different errors (error message body is passed through from EIS) | 400    |
| No data found for VRN                                                                                                                           | 404    |
| Internal server error                                                                                                                           | 500    |
| Dependent systems are not available                                                                                                             | 503    |

The data returned is outlined in v1.1.0 of the GetPenaltyDetails API specification.

## Authentication

This service is protected by service-to-service authentication. Currently, only BTA, VATVC and other Penalties services can call this service.

A valid token needs to be created in the internal auth service to be used in calls from the Penalties backend.

See https://github.com/hmrc/internal-auth#endpoints on how to create a valid token to use in calling services.

## Testing

This service can be tested with SBT via `sbt test it:test`

To run coverage and scalastyle, please run: `sbt clean scalastyle coverage test it:test coverageReport`

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
