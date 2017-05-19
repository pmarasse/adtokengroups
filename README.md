Description
=========

ADTokenGroups is a little component used to translate binary SID fetched from tokenGroups AD attribute to DN of the corresponding group. As it accepts byte[] SID, there is no need to translate binary data fetched from AD before querying this component.

The main implementation uses JCache API (tested with EhCache implementation) to avoid unnecessary load on active directory.

Dependencies
------------

  * Ldaptive 1.2
  * JCache 1.0.0
  * slf4j API 1.7

Changelog
=========

Version 1.0.0-SNAP
------------------

  * Updated dependency from Spring LDAP 1.3 to Ldaptive 1.2
  * Updated dependency from EhCache 2.5 to JCache 1.0.0
  * Tests with both EhCache 3.2 and HazelCast 3.8 implementations
  * Test with Spring Framework 4.3
  * Updated API of CachingADTokenGroupsRegistry a cache manager or cache instance can be provided

Version 0.1.4
-------------

  * Added a setter to be more kind with spring IoC

Version 0.1.3
-------------

  * First fully usable version with stabilized interface

