java -Dhttp.agent="Googlebot/2.1" -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=10000 -Djava.util.logging.config.file=log4j.properties -Xms512m -Xmx512m -cp .:bin:lib/* -Dwordnet.database.dir=wordnet/dict -DBTEXT_HOME=. com.alienobject.textpipe.$1 "$2" $3 $4


