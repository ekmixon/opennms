#!/usr/bin/python

# Try and import the system Jython libraries to allow this script
# to function under Jython.
import sys
sys.path.append('/usr/share/jython/Lib')

import re
import urllib2
import xml.dom.minidom

# Edit these constants to match your installation if necessary
FILE_SEPARATOR = '/'
OPENNMS_SHARE_DIR = f'{FILE_SEPARATOR}var{FILE_SEPARATOR}opennms'
RRD_FILE_EXTENSION = '.jrb'
OPENNMS_REST_HOST = 'http://127.0.0.1:8980/'
OPENNMS_REST_URL = f'{OPENNMS_REST_HOST}opennms/rest'
OPENNMS_REST_USER = 'admin'
OPENNMS_REST_PASSWORD = 'admin'

def configureRrdPaths(rrdDir):
    global OPENNMS_SHARE_DIR
    OPENNMS_SHARE_DIR = rrdDir

# Parse an RRD file path into metadata about the RRD datasources
def parseRrdPath(path):
    if not (
        pathMatch := re.match(
            f'^{OPENNMS_SHARE_DIR}{FILE_SEPARATOR}rrd{FILE_SEPARATOR}(.*){RRD_FILE_EXTENSION}$',
            path,
        )
    ):
        raise f"Path does not match expected format: {path}"
        # Strip off the directory prefix and file extension
    path = pathMatch[1]

    # This dict object will contain a list of key-value pairs that give
    # extra metadata about the RRD from the RRD path, including node and
    # interface-level information
    retval = {}
    if re.match('^response', path):
            # Service response time
        path = re.match(f'^response{FILE_SEPARATOR}(.*)$', path)[1]
        pathElements = path.split(FILE_SEPARATOR)
        if len(pathElements) != 2:
            raise f"Unexpected response time path: {path}"
        retval['interface.ipAddress'] = pathElements[0]
                # TODO Use REST to fetch more info about IP address
        retval['metric.label'] = f'{pathElements[1]}ResponseTime'
    elif re.match('^snmp', path):
            # SNMP metric
        path = re.match(f'^snmp{FILE_SEPARATOR}(.*)$', path)[1]
        pathElements = re.split(FILE_SEPARATOR, path)
            # Interface-specific metric
        if (len(pathElements) == 3):
            retval['node.id'] = pathElements[0]
            # Use REST to fetch more info about the node
            fetchNode(retval['node.id'], retval)
            interface = pathElements[1]
            retval['interface.label'] = split('-', interface)[0]
            retval['interface.macAddress'] = split('-', interface)[1]
            # TODO Use REST to fetch more info about the interface
            retval['metric.label'] = pathElements[2]
        elif (len(pathElements) == 2):
            retval['node.id'] = pathElements[0]
            # Use REST to fetch more info about the node
            fetchNode(retval['node.id'], retval)
            retval['metric.label'] = pathElements[1]
        else:
            raise f"Unexpected SNMP path: {path}"
    return retval

def fetchNode(id, attributeDict):
    # TODO Add parameter checking
    passwordManager = urllib2.HTTPPasswordMgrWithDefaultRealm()
    passwordManager.add_password(None, OPENNMS_REST_URL, OPENNMS_REST_USER, OPENNMS_REST_PASSWORD)
    authHandler = urllib2.HTTPBasicAuthHandler(passwordManager)
    opener = urllib2.build_opener(authHandler)
    urllib2.install_opener(opener)
    #print "Target URL: " + OPENNMS_REST_URL + '/nodes/' + id
    response = urllib2.urlopen(f'{OPENNMS_REST_URL}/nodes/{id}')
    nodeXml = xml.dom.minidom.parse(response)
    node = nodeXml.documentElement
    # Append attributes from the REST response to the attributeDict
    attributeDict['node.label'] = node.getAttribute('label')
    attributeDict['node.createTime'] = node.getElementsByTagName('createTime')[0].childNodes[0].nodeValue
    attributeDict['node.lastCapsdPoll'] = node.getElementsByTagName('lastCapsdPoll')[0].childNodes[0].nodeValue
    attributeDict['node.sysContact'] = node.getElementsByTagName('sysContact')[0].childNodes[0].nodeValue
    attributeDict['node.sysDescription'] = node.getElementsByTagName('sysDescription')[0].childNodes[0].nodeValue
    attributeDict['node.sysLocation'] = node.getElementsByTagName('sysLocation')[0].childNodes[0].nodeValue
    attributeDict['node.sysName'] = node.getElementsByTagName('sysName')[0].childNodes[0].nodeValue
    attributeDict['node.sysObjectId'] = node.getElementsByTagName('sysObjectId')[0].childNodes[0].nodeValue

# Test data
#print parseRrdPath("/var/opennms/rrd/response/127.0.0.1/ssh.jrb")
#print parseRrdPath("/var/opennms/rrd/snmp/1/myMetric.jrb")
