Build:
```
docker build -t docker-build .
```

Example usage:
```
# Make ivy and maven repositories (these can be reused)
mkdir -p ~/docker-repo/repository/
mkdir -p ~/docker-repo/.ivy2/
```

To build pentaho/pentaho-kettle's plugins/pdi-pur-plugin:
```
# Ant/Ivy build
docker run -e "API_TOKEN=$GITHUB_API_TOKEN" -v ~/docker-repo/.ivy2:/home/buildguy/.ivy2 -v ~/docker-repo/repository/:/home/buildguy/.m2/repository -t -i --rm docker-build -r pentaho/pentaho-kettle -p 1856 -b 'ant -f plugins/pdi-pur-plugin/build.xml clean-all resolve jacoco jacoco-integration checkstyle' -c 'rm -r ~/.ivy2/local || echo "no publish local to remove"'
```
To build pentaho/big-data-plugin's legacy submodule:
```
#Maven build
docker run -e "API_TOKEN=$GITHUB_API_TOKEN" -v ~/docker-repo/.ivy2:/home/buildguy/.ivy2 -v ~/docker-repo/repository/:/home/buildguy/.m2/repository -t -i --rm docker-build -r pentaho/big-data-plugin -p 549 -b 'mvn -f legacy/pom.xml clean install site' -c 'mvn build-helper:remove-project-artifact'
```
