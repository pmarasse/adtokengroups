Description
=========

ADTokenGroups is a little component used to translate binary SID fetched from tokenGroups AD attribute to DN of the corresponding group. As it accepts byte[] SID, there is no need to translate binary data fetched from AD before querying this component.

The main implementation uses EhCache to avoid unnecessary load on active directory.

Dependencies
------------

* Spring LDAP
* EhCache

Changelog
=========

Version 0.1.4
-------------

* Added a setter to be more kind with spring IoC

Version 0.1.3
-------------

* First fully usable version with stabilized interface

