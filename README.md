# shibboleth_django-freeradius_login

This project implements a JAAS LoginModule of Java which permits a Shibboleth idp server to authenticate with the module 
[django-freeradius](https://github.com/openwisp/django-freeradius).



## Requirements

This project works with a Shibboleth idp server and django-freeradius module, so see the following links for the documentations:
* [django-freeradius](http://django-freeradius.readthedocs.io/en/latest/index.html);
* [Shibboleth idp](https://wiki.shibboleth.net/confluence/display/IDP30/Installation).

Furthermore, this module use the [Unirest](http://unirest.io/java.html) library so it's necessary have a .jar file of this.
See this page for generate it from the library:
[install unirest-java](https://konghq.com/blog/installing-unirest-java-with-the-maven-assembly-plugin/).



## Seup

Clone the repository of the project in the `/root` folder of the Shibboleth idp server:

```
git clone https://github.com/AlessiaVe/shibboleth_django-freeradius_login.git
```

Move into the `src` directory:
```
cd src
```
### jaas.config
Create a file `jaas.config` in this directory:

```
ShibUserPassAuth {
 Django.jaas.DjangoLoginModule sufficient
   djangoUrl="http://ip:port/api/authorize/";
};

```
This file sets up the DjangoLoginModule as sufficient for the authentication of the user. The option djangoUrl is the url
of the Django REST framework to the authorize page:
* ip is the ip of the machine where there is the django server;
* port is the port where we have set up the rest interface of Django.

### Unirest library

After generate the .jar file, raname it like `unirest.jar` and copy it in the `src` folder.


### Build the project

This code has to be compiled and move to the principal directory of the Java enviroment. So I suggest to make a `build.sh` 
in the `src` folder to do it automatically. The file is like this:

```
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre

cp /root/shibboleth_django-freeradius_login/src/unirest.jar              $JAVA_HOME/lib/ext/

javac -cp /root/shibboleth_django-freeradius_login/src/unirest.jar Django/jaas/DjangoLoginModule.java
javac Django/jaas/DjangoPrincipal.java
jar cvf Django.jar Django/jaas/*.class

cp /root/shibboleth_django-freeradius_login/src/Django.jar               $JAVA_HOME/lib/ext/
cp /root/shibboleth_django-freeradius_login/src/jaas.config              /opt/shibboleth-idp/conf/authn
```

This file copies the unirest.jar into the JAVA_HOME directory, copiles the two java files with the dependency and 
copies the jar and the jaas.config files into the JAVA_HOME.


## Run the project

To start use the module, It's necessary run the `build.sh` file with:
```
./build.sh
```
and restart the server with:
```/root/restart.sh```

After that, the idp authenticates only the Django users.
Hope you enjoy!
