dockerRun: ##Spin up docker containers for MA with extension, controller and other apps
	@echo "------- Starting controller -------"
	docker-compose up -d --force-recreate controller

#wait until controller and ES installation completes
	sleep 600
	@echo "------- Controller started -------"

#bash into the controller, change props to enable port 9200
	docker exec controller /bin/bash -c "sed -i s/ad.es.node.http.enabled=false/ad.es.node.http.enabled=true/g events-service/processor/conf/events-service-api-store.properties"
#restart ES to make the changes reflect
	docker exec controller /bin/bash -c "pa/platform-admin/bin/platform-admin.sh submit-job --platform-name AppDynamicsPlatform --service events-service --job restart-cluster"
	sleep 60
	/bin/sh src/integration-test/resources/conf/apikeys.sh

#start machine agent
	@echo ------- Starting machine agent -------
	docker-compose up --force-recreate -d --build machine
	@echo ------- Machine agent started -------

dockerStop: ##Stop and remove all containers
	@echo ------- Stop and remove containers, images, networks and volumes -------
	docker-compose down --rmi all -v --remove-orphans
	docker rmi dtr.corp.appdynamics.com/appdynamics/machine-agent:latest
	docker rmi alpine
	@echo ------- Done -------

sleep: ##sleep for x seconds
	@echo Waiting for 5 minutes to read the metrics
	sleep 300
	@echo Wait finished

dockerClean: ##Clean any left over containers, images, networks and volumes
	@if [[ -n "`docker ps -q`" ]]; then \
	docker stop `docker ps -q`; \
	fi
	docker rm -f `docker ps -a -q` || echo 0
	docker system prune -f -a --volumes