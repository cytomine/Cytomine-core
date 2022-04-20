#!/bin/bash

 set -o xtrace
 set -o errexit

#alter database docker is_template false;
# psql -p 5452 -h localhost -U docker -d postgres
#postgres=# drop database docker;
PGPASSWORD=docker
#psql -p 5452 -h localhost -U docker < 'DROP TABLE IF EXISTS cytomine'
psql -p 5452 -h localhost -U docker < /home/lrollus/dataset/postgres_06-04-2022_23_40-v3.2.0.sql


#mongo --host localhost --port 27037 --username mongoadmin --password secret --authenticationDatabase admin cytomine \
#--eval "db.persistentProjectConnection.drop()"
#mongoimport --host localhost --port 27037 --username mongoadmin --password secret --authenticationDatabase admin -d cytomine \
#-c persistentProjectConnection --file /home/lrollus/dataset/mongodb_06-04-2022_02_30/cytomine/persistentProjectConnection.metadata.json
#mongorestore --host localhost --port 27037 --username mongoadmin --password secret --authenticationDatabase admin  -d cytomine \
#-c persistentProjectConnection /home/lrollus/dataset/mongodb_06-04-2022_02_30/cytomine/persistentProjectConnection.bson

