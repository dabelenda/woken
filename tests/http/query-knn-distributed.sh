#!/bin/bash

http -v --verify=no -a admin:WoKeN --timeout 180 POST http://localhost:8087/mining/job \
         user:='{"code":"user1"}' \
         variables:='[{"code":"brainstem"}]' \
         grouping:='[]' \
         covariables:='[{"code":"leftcaudate"}]' \
         filters:='""' \
         datasets:='[{"code":"node1"},{"code":"node2"}]' \
         algorithm:='{"code":"knn", "name": "KNN", "parameters": []}'