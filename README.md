victims-api
===========
[![Build Status](https://travis-ci.org/victims/victims-api.svg)](https://travis-ci.org/victims/victims-api/)

Public API to the victims data set.

Building
========

For all targets run ``make help``

Dynamic
-------
1. Install dependencies via ``govendor`` by running: ``make deps``
2. Run ``make victims-api``
3. Execute ``./victims-api run``

Static
------
1. Install dependencies via ``govendor`` by running: ``make deps``
2. Run ``make static-victims-api``
3. Execute ``./victims-api run``

Image
-----
1. Run ``make image``


Running
=======

Manually
--------
To find out the available flags run:

```
$ victims-api run --help
```

Example:
```
$ victims-api run \
  --mongodb-database victims \
  --mongodb-host 127.0.0.1 \
  --mongodb-user myuser \   # Optional
  --mongodb-password secret # Optional
```


Environment
-----------
**TODO**: Update environment vars to either use a URI OR add a ``MONGODB_HOST`` environment variable.

Set the following environment variables:

- ``MONGODB_USER``: User to connect to the databse with
- ``MONGODB_PASSWORD``: Password to connect to the database with
- ``MONGODB_DATABASE``: Databse to use (usually ``victims``)


```
$ ./victims-api --mongodb-host 127.0.0.1 --mongodb-use-env
```
