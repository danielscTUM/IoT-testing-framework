#!/bin/bash
cd "$(dirname "$0")"

echo "Start"

NUMBER_OF_PODS=${1}
IMAGE=${2}

BINARY="kubectl"
OFFSET=0

# Start Redis service
if [[ $( ${BINARY} get deployment redis-master | grep redis-master | wc -l ) -lt 1 ]]; then
  echo "Start redis deployment"
  ${BINARY} apply -f redis-master-deployment.yaml
fi
if [[ $( ${BINARY} get service redis-master | grep redis-master | wc -l ) -lt 1 ]]; then
  echo "Start redis service"
  ${BINARY} apply -f redis-master-service.yaml
fi

COUNTER=0
PODS=()
while [[ ${COUNTER} -lt ${NUMBER_OF_PODS} ]]; do
  PODS+=(${COUNTER})
  ((COUNTER+=1))
done

for POD_ID in ${PODS[@]}
do
    OFFSET_FOR_POD=$(( POD_ID*(${OFFSET}) ))
    cat > device_simulator_config_${POD_ID}.yaml <<EOL
apiVersion: v1
kind: ConfigMap
metadata:
  name: device-simulator-config-${POD_ID}
data:
  test_env: "kubernetes"

  # Note these numbers are per instance (see device_simulator_job.yaml for number of instances)
  pod_number: "${POD_ID}"
  number_of_pods: "${NUMBER_OF_PODS}"
EOL
    cat device_simulator_job_template.yaml | sed -e "s/\$DEVICE_SIMULATOR_NUMBER/${POD_ID}/" -e "s/\$IMAGE/${IMAGE}/" > device_simulator_job_${POD_ID}.yaml

    if [[ $( ${BINARY} --kubeconfig=${CONFIG} get job device-simulator-${POD_ID} | grep device-simulator-${POD_ID} | wc -l ) -gt 0 ]]; then
      ${BINARY} --kubeconfig=${CONFIG} delete -f device_simulator_config_${POD_ID}.yaml -f device_simulator_job_${POD_ID}.yaml
    fi

    ${BINARY} --kubeconfig=${CONFIG} apply -f device_simulator_config_${POD_ID}.yaml -f device_simulator_job_${POD_ID}.yaml

    rm device_simulator_job_${POD_ID}.yaml device_simulator_config_${POD_ID}.yaml
done

finished=0
while [[ ${finished} == 0 ]]; do
  finished=1
  STATUS=$(kubectl get pod -l job-name=device-simulator-0 -o jsonpath="{.items[0].status.phase}")
  POD=$(kubectl get pod -l job-name=device-simulator-0 -o jsonpath="{.items[0].metadata.name}")
  stat=$(kubectl exec ${POD} -- cat status)
  if [[ ! ${stat} == finished ]]
    then finished=0
  fi
  echo Wait
  sleep 1
done

kubectl cp ${POD}:result result
sleep 1
kubectl exec ${POD} -- touch finish

echo "Finished"