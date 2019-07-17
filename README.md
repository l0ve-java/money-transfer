# Start application
~~~
java -Dfile.encoding=UTF-8 -jar money-transfer-app.jar
~~~
# Configuration
Application comes with default [configuration](src/main/resources/application.properties).

You can override it by putting your `application.properties` file to work directory or pass as run argument:
~~~
java -Dfile.encoding=UTF-8 -jar money-transfer-app.jar "mydirectory/money-transfer/production.properties"
~~~
# API examples
## Get account information
~~~
GET /account/{id} 
~~~
## Get account balance
~~~
GET /account/{id}/balance 
~~~
## Create new active account
~~~
PUT /account

{
    "status": "ACTIVE"
}
~~~
## Change account status
~~~
POST /account

{
    "id": 1,
    "status": "ACTIVE"
}
~~~
## Create one-way credit operation (topup balance)
~~~
POST /operation/transfer

{
    "targetAccount": 1,
    "amount": 100
}
~~~
## Create one-way debit operation (withdraw funds)
~~~
POST /operation/transfer

{
    "sourceAccount": 1,
    "amount": 100
}
~~~
## Create money transfer operation
~~~
POST /operation/transfer

{
    "sourceAccount": 1,
    "targetAccount": 2,
    "amount": 100
}
~~~