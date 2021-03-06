DROP TABLE IF EXISTS `Plugins`;
CREATE TABLE `Plugins` (
  `PluginName` varchar(64) NOT NULL default '',
  `URL` mediumtext NOT NULL,
  `CorePlugin` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`PluginName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `_objectdb_plugins_alias_aliasobject`;
CREATE TABLE `_objectdb_plugins_alias_aliasobject` (
  `id` int(11) NOT NULL auto_increment,
  `name` text,
  `converted` text,
  `owner` text,
  `locked` tinyint(4) NOT NULL default '0',
  `help` text,
  `core` text,
  PRIMARY KEY  (`id`),
  KEY `name__index` (`name`(16)),
  KEY `converted__index` (`converted`(16)),
  KEY `owner__index` (`owner`(16)),
  KEY `locked__index` (`locked`),
  KEY `help__index` (`help`(16)),
  KEY `core__index` (`core`(16))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

DROP TABLE IF EXISTS `_objectdb_plugins_autojoin_channelobj`;
CREATE TABLE `_objectdb_plugins_autojoin_channelobj` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` text,
  PRIMARY KEY (`id`),
  KEY `name__index` (`name`(16))
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
INSERT INTO _objectdb_plugins_autojoin_channelobj (`name`) VALUES ('#bots');

DROP TABLE IF EXISTS `GroupMembers`;
CREATE TABLE `GroupMembers` (
  `GroupID` int(11) unsigned default NULL,
  `MemberID` int(11) unsigned default NULL,
  UNIQUE KEY `GroupID` (`GroupID`,`MemberID`),
  KEY `MemberID` (`MemberID`),
  KEY `GroupID_2` (`GroupID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `History`;
CREATE TABLE `History` (
  `LineID` int(11) unsigned NOT NULL auto_increment,
  `Type` varchar(50) NOT NULL default '',
  `Nick` varchar(64) NOT NULL default '',
  `Hostmask` varchar(128) NOT NULL default '',
  `Channel` varchar(32) default NULL,
  `Text` text NOT NULL,
  `Time` bigint(20) NOT NULL default '0',
  `Random` tinyint(4) NOT NULL default '0',
  PRIMARY KEY  (`LineID`),
  KEY `Nick` (`Nick`),
  KEY `Channel` (`Channel`),
  KEY `Time` (`Time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `LoadedPlugins`;
CREATE TABLE `LoadedPlugins` (
  `Name` varchar(255) NOT NULL default '',
  `Source` longtext NOT NULL,
  PRIMARY KEY  (`Name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `UserNodePermissions`;
CREATE TABLE `UserNodePermissions` (
  `NodeID` int(11) unsigned NOT NULL default '0',
  `Type` varchar(50) NOT NULL default '',
  `Permission` varchar(80) NOT NULL default '',
  `Action` varchar(30) NOT NULL default '',
  UNIQUE KEY `NodeID_2` (`NodeID`,`Type`,`Permission`,`Action`),
  KEY `NodeID` (`NodeID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Table storing plugin permissions';

DROP TABLE IF EXISTS `UserNodes`;
CREATE TABLE `UserNodes` (
  `NodeID` int(11) unsigned NOT NULL auto_increment,
  `NodeName` varchar(32) NOT NULL default '',
  `NodeClass` tinyint(3) unsigned NOT NULL default '0',
  PRIMARY KEY  (`NodeID`),
  UNIQUE KEY `NodeName` (`NodeName`,`NodeClass`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `GroupMembers`
  ADD CONSTRAINT `GroupMembers_ibfk_1` FOREIGN KEY (`GroupID`) REFERENCES `UserNodes` (`NodeID`),
  ADD CONSTRAINT `GroupMembers_ibfk_2` FOREIGN KEY (`MemberID`) REFERENCES `UserNodes` (`NodeID`);

ALTER TABLE `UserNodePermissions`
  ADD CONSTRAINT `UserNodePermissions_ibfk_1` FOREIGN KEY (`NodeID`) REFERENCES `UserNodes` (`NodeID`);

-- These plugins are really required to have a functional bot. More can be loaded once these are.

INSERT INTO `Plugins` VALUES ('Autojoin', 'choob-plugin:/Autojoin.java', 1);
INSERT INTO `Plugins` VALUES ('Alias', 'choob-plugin:/Alias.java', 1);
INSERT INTO `Plugins` VALUES ('Help', 'choob-plugin:/Help.java', 1);
INSERT INTO `Plugins` VALUES ('NickServ', 'choob-plugin:/NickServ.java', 1);
INSERT INTO `Plugins` VALUES ('Options', 'choob-plugin:/Options.java', 1);
INSERT INTO `Plugins` VALUES ('Plugin', 'choob-plugin:/Plugin.java', 1);
INSERT INTO `Plugins` VALUES ('Security', 'choob-plugin:/Security.java', 1);

-- Need to give Nickserv enough permissions to be able to talk to nickserv.

INSERT INTO `UserNodes` VALUES (1, 'root', 3);
INSERT INTO `UserNodes` VALUES (2, 'NickServ', 2);
INSERT INTO `UserNodes` VALUES (3, 'Plugin', 2);
INSERT INTO `UserNodes` VALUES (4, 'Security', 2);
INSERT INTO `UserNodes` VALUES (5, 'Alias', 2);
INSERT INTO `UserNodes` VALUES (6, 'Help', 2);
INSERT INTO `UserNodes` VALUES (7, 'Options', 2);

INSERT INTO `GroupMembers` VALUES (1, 2);
INSERT INTO `GroupMembers` VALUES (1, 3);
INSERT INTO `GroupMembers` VALUES (1, 4);
INSERT INTO `UserNodePermissions` VALUES (1, 'java.security.AllPermission', '', '');

