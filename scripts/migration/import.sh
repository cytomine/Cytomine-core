#!/bin/bash

 set -o xtrace
 set -o errexit


for COLLECTION in annotationAction lastConnection lastUserPosition persistentConnection persistentImageConsultation persistentProjectConnection persistentUserPosition
do
  mongo --host mongodb --port 27017 --username mongoadmin --password secret --authenticationDatabase admin cytomine \
  --eval "db.$COLLECTION.drop()"
  mongoimport --host mongodb --port 27017 --username mongoadmin --password secret --authenticationDatabase admin -d cytomine \
  -c $COLLECTION --file /datasets/indexes/$COLLECTION.metadata.json
  mongorestore --host mongodb --port 27017 --username mongoadmin --password secret --authenticationDatabase admin  -d cytomine \
  -c $COLLECTION /datasets/$COLLECTION.bson
done


#mongo --host localhost --port 27037 --username mongoadmin --password secret --authenticationDatabase admin cytomine \
#--eval "db.persistentProjectConnection.drop()"
#mongoimport --host localhost --port 27037 --username mongoadmin --password secret --authenticationDatabase admin -d cytomine \
#-c persistentProjectConnection --file /home/lrollus/dataset/mongodb_06-04-2022_02_30/cytomine/persistentProjectConnection.metadata.json
#mongorestore --host localhost --port 27037 --username mongoadmin --password secret --authenticationDatabase admin  -d cytomine \
#-c persistentProjectConnection /home/lrollus/dataset/mongodb_06-04-2022_02_30/cytomine/persistentProjectConnection.bson

