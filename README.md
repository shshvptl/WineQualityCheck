Below are the steps to create EC2 instances on Amazon aws:
1. Create Amazon AWS Educate Account
2. Once Account is created, sign in to aws educate account
3. Click on AWS Account -> AWS Educate Starter Account
4. Open AWS Console
5. Search for EC2 under services
6. Click on Instances under Instance on the  left side
7. Launch Instance -> Select Amazon Ubuntu  -> Select general purpose t2.micro (Free tier eligible) -> Next:Configure Instance Details -> put 4 in Number of instances -> Next: Add Storage-> Next: Add Tags -> Next: Configure Security Group -> Choose source as anywhere for All traffic -> Create new pair key ->  download new pair key -> once all instances are running you can change the name of all instances

Below are the steps to setup spark cluster in EC2 instances:
1. login to EC2 instance as ubuntu user
2. execute below command to update all 
	sudo apt-get update
3. execute  below command to install java	
	sudo apt-get -y install openjdk-8-jdk-headless
4. execute below command to check java version
	java -version
5. execute below command to download spark hadoop 
	wget https://archive.apache.org/dist/spark/spark-2.4.7/spark-2.4.7-bin-hadoop2.7.tgz -P ~/server
6. execute below command to untar downloaded file
	sudo tar xvzf spark-2.4.7-bin-hadoop2.7.tgz
7. execute below command to set up JAVA_HOME varible
	export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
8. execute below command to start master	
	cd ~/server
	./spark-2.4.7-bin-hadoop2.7/sbin/start-master.sh
9. execute below command to start slave
	cd ~/server
	./spark-2.4.7-bin-hadoop2.7/sbin/start-slave.sh spark://ip-172-31-16-165.ec2.internal:7077
	
Follow all above steps in each EC2 instance. and start only slave on other EC2 instance after downloading spark hadoop.

To run application on spark cluster without docker, execute below command:
	/home/ubuntu/server/spark-2.4.7-bin-hadoop2.7/bin/spark-submit --class com.amazon.aws.PredictWineQualityEngine ./CS643_Programming_Assignment2-1.1.0-SNAPSHOT.jar

Below are the command to create docker image:
1. execute below command to install docker
	sudo apt install docker.io
2. execute below command to start the docker
	sudo service docker start
3. execute below command to check status of docker
	sudo service docker status
4. execute below command to add ubuntu to docker group
	sudo usermod -a -G docker ubuntu
5. restart instance
6. to create docker image for the app, create dockerfile
7. once docker file is created, execute below command to create docker image
	docker build -t <imagename> .
8. once docker image is created, execute below command to start the build
	docker run -t assignment2_v41
9. execute below command to check the status of the container
	docker container ls
10. execute below command to check container log
	docker ps -a

Below are the step to push docker image to dockerhub:
1. create a new account on https://hub.docker.com/ if you dont have it
2. execute below command to login to your dockerhub from EC2 instance
	docker login -u <dockerhub_username>
3. put dockerhub password
4. execute below command to add a tag to your docker image
	docker tag <docker_imagename>:latest <dockerhub_ussername>/dockerhub:<tag_name>
	Ex. docker tag assignment2_v40:latest shshvptl9/dockerhub:myfirstimagepush
5. execute below command to push your image to dockerhub
	docker push <dockerhub_ussername>/dockerhub:<tag_name>
	Ex. docker push shshvptl9/dockerhub:myfirstimagepush
	
To run this application with docker image, execute below command:
	docker run -t -p 5000-5010:5000-5010 -e SPARK_MASTER="spark://ip-172-31-16-165.ec2.internal:7077" -e SPARK_DRIVER_HOST="ip-172-31-27-173.ec2.internal" assignment2_v38
	OR docker run -t <docker_imagename>


