 
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

#### `GET        /vat/penalties/summary/:vrn`
Retrieve penalties summary information belonging to a vrn.

Example vrn: `123456789`

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

| Scenario | Status |
| --- | --- |
| Successful retrieval | 200 |
| Invalid vrn format | 400 |
| No data found for vrn | 404 |

#### `GET        /etmp/penalties/:enrolmentKey`
Get data for penalties belonging to an enrolment key. 

Example enrolmentKey format: `HMRC-MTD-VAT~VRN~123456789`
 
| Scenario | Status |
| --- | --- |
| Successful retrieval | 200 |
| Invalid vrn format | 400 |
| No data found for vrn | 404 |

### Appeals:
#### `GET        /appeals-data/late-submissions`

Get Late submission penalty data for an appeal.

Takes in a query params `penaltyId` and `enrolement key`

Url format - `/appeals-data/late-submissions?{penaltyId}=[idvalue]&{enrolementKey}=[key]`

Example url - `/appeals-data/late-submissions?penaltyId=1234567890&enrolementKey=HMRC-MTD-VAT~VRN~123456789`

| Scenario | Status |
| --- | --- |
| Successful retrieval | 200 |
| No data found for penaltyId | 404 |
| Internal server error | 500 |

#### `GET        /appeals-data/late-payments`

Get Late payment penalty data for an appeal.

Takes in a query params `penaltyId`, `enrolement key` and `isAdditional`. 

`isAdditional` is set to true if the penalty is an additional penalty. 

Url format - `/appeals-data/late-submissions?{penaltyId}=[idvalue]&{enrolementKey}=[value]&{isAdditional}=[booleanValue]`

Example url - `/appeals-data/late-submissions?penaltyId=1234567890&enrolementKey=HMRC-MTD-VAT~VRN~123456789&isAdditional=false`

| Scenario | Status |
| --- | --- |
| Successful retrieval | 200 |
| No data found for penaltyId | 404 |
| Internal server error | 500 |

#### `GET        /appeals-data/reasonable-excuses`

Get list of reasonable excuses used to make an appeal.

| Scenario | Status |
| --- | --- |
| Successful retrieval | 200 |


#### `POST       /appeals/submit-appeal`

Submit an appeal for a penalty.

Takes in query params `enrolementKey`, `isLPP`, `penaltyNumber` and `correlationId`.

Url format - `/appeals/submit-appeal?{enrolmentKey}=[keyValue]&{isLPP}=[boolValue]&{penaltyNumber}=[penaltyId]&{correlationId}=[value]`

Example url - `/appeals/submit-appeal?enrolmentKey=HMRC-MTD-VAT~VRN~224060020&isLPP=false&penaltyNumber=123456786&correlationId=a8010aef-9253-45a8-b8ac-c843dc2d3318`

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
            "startDateOfEvent": "2021-04-23T18:25:43.511Z",
            "reportedIssueToPolice": true,
            "lateAppeal": true,
            "lateAppealReason": "Reason",
            "isClientResponsibleForSubmission": false,
            "isClientResponsibleForLateSubmission": true
    }
}
```

| Scenario | Status |
| --- | --- |
| Successful post | 200 |
| Invalid body received / Failed to parse to model | 400 |
| Internal server error | 500 |

### Compliance
#### `GET        /compliance/des/compliance-data`
Get compliance data of penalty for vrn in a period.

Takes in query params `vrn`, `fromDate` and `toDate`

Url format - `/compliance/des/compliance-data?{vrn}=[value]&{fromDate}=[date]&{toDate}=[date]`

Example url - `/compliance/des/compliance-data?vrn=1234567890&fromDate=2020-01-01&toDate=2020-03-31`

| Scenario | Status |
| --- | --- |
| Successful retrieval | 200 |
| Failed to parse to model | 400 |
| No data found for vrn | 404 |
| Internal server error | 500 |

## Testing

This service can be tested with SBT via `sbt test it:test`

To run coverage and scalastyle, please run: `sbt clean scalastyle coverage test it:test coverageReport`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
