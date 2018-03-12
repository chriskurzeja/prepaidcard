# Prepaid Card
A simple implementation of a REST API for a prepaid card onto which:

### The user can
- Create a card. On success the cardId is returned.
- Load money. On success a confirmation message is returned.
- See total loaded money. On success the total amount loaded in pence is returned.
- See available funds. On success the total amount available in pence is returned.
- See blocked funds. On success the total amount blocked in pence is returned.
- See transactions. The list of transactions for the cardId is returned.

The user API has a route for each transaction.

### The merchant can
- **authorise** transactions and block the funds. On success a summary of the state for the current transaction is returned.
- **capture** funds from an authorised transaction. On success a summary of the state for the current transaction is returned.
- **reverse** the transaction and change the amount blocked. On success a summary of the state for the current transaction is returned.
- **refund** funds that have been captured. On success a summary of the state for the current transaction is returned.

The merchant API has a single route for all transactions. The keyword in bold in the list above is the action.

There is also an admin API that can list all the current users and provide their cardId.

A Swagger2 UI is available to interact with the REST api.

# Todo List
Given additional resource I would consider the following improvements:
 - Audit the return values for the REST API. Is the HTTP code correct? We shouldn't be sending *BadRequest* back when a transaction isn't valid as the request is fine.
 - Consider using *Lombok* or *Kotlin* for the data classes - reduces boiler plate code. If not using either of these then consider using the Apache Commons reflection builders for hashcode, equals, and toString.
 - Add unit testing for the REST API using the *SpringMVC standaloneSetup*.
 - Consider how to break down *TransactionServiceTest* into multiple test files per transaction type?
 - Implement a single page application using *React* that can visualise the user data. See below.
 - Seek some peer review of existing code/structure/models.
 - Consider the problem of scaling. What changes are required to support more users. This would involve things like caching current state properly using something like *Caffeine* that will also provide expiry. What changes would be required to support horizontal scaling? Are we using databases sensibly or should we bring in something like the *CQRS* pattern. What testing can we add to evaluate performance with multiple users interacting with the system and evaluate throughput (*Gatling*)?
 - A merchant portal enabling administration of transactions.
 
 # User Balance Visualisation
 If I were to implement a visualisation page I may implement the following:
 - summary of user's current funds: available, blocked in numeric form
 - Some form of visualisation to accompany the above (possibly using *VictoryJS*). Would this be individual charts or use the *VictoryStack* chart to overlay the information. 
 - How much of a picture of the above can we build for the user? Specifically can we build charts that have historic data? Is that worthwhile?
 - Some form of visualisation breaking down the amount spend at common merchants? Is there a way to turn this data into something more generic like 'food', 'drink' etc.
 - A table of transactions with a key row of highlevel transaction summary with the potential to display more information if required (but would we even want the user to have this amount of granularity about their data).
 
 
 
