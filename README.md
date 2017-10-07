victims-api
===========

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
