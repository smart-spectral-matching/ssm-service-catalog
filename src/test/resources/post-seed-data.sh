#!/bin/bash
  
# Either get server IP:port from CLI or default to localhost
input_ip=$1
server_ip="${input_ip:=http://localhost:8080}"

# Create the map to hold key=fileanmes value=dataset urls
declare -A datasets

# Loop over files, upload to REST API, and add URLs to the datasets map
for jsonld in "simple.input.jsonld" "scidata_nmr_abbreviated.input.jsonld" "studtite.input.jsonld" 
do
    dataset_uuid=$(curl -X POST "${server_ip}/datasets" | jq -r .uuid)
    model_uuid=$(curl -X POST "${server_ip}/datasets/${dataset_uuid}/models" -H "Content-Type: application/json" -d @studtite.jsonld | jq -r .uuid)
    name=$(echo ${jsonld} | cut -d'.' -f 1)
    datasets["${name}"]="${server_ip}/datasets/${dataset_uuid}/models/${model_uuid}"
done

# Print out the results
for name in "${!datasets[@]}"
do
    printf "%s url:\n" "$name"
    printf "    %s\n" "${datasets[$name]}"
done
