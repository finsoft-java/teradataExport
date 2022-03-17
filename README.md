# teradataExport
Export Teradata tables to SQL

Standard tools (such as SQL Assistant or Teradata Studio) do not allow exporting a table in SQL (INSERT) format. This CLI tool allows it.

You may export any SQL query, not only a single table.

## Build
From CLI, run:

    mvn package
    
that will generate `target/teradataExport.jar`

## Usage
From CLI, run:

    java -jar TeradataExport.jar <jdbc connection url> <query>  > myfile.sql
  
es.

    java -jar TeradataExport.jar "jdbc:teradata://myserver/USER=myuser,PASSWORD=mypassword,DATABASE=mydefaultdatabase"  "SELECT * FROM MYTABLE" > myfile.sql
