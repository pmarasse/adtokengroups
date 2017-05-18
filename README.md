Description
=========

ADTokenGroups is a little component used to translate binary SID fetched from tokenGroups AD attribute to DN of the corresponding group. As it accepts byte[] SID, there is no need to translate binary data fetched from AD before querying this component.

The main implementation uses JCache API (tested with EhCache implementation) to avoid unnecessary load on active directory.

Dependencies
------------

* Spring LDAP 2.3.x
* JCache 1.0.0

Changelog
=========

Version 1.0.0-SNAP
------------------

* Drop EhCache dependency in favor of JCache API
* Updated Spring LDAP 1.3 to 2.3
* Updated EhCache 2.5 to JCache 1.0.0

Version 0.1.4
-------------

* Added a setter to be more kind with spring IoC

Version 0.1.3
-------------

* First fully usable version with stabilized interface

