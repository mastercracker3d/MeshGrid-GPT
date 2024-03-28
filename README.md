# MeshGrid-GPT
GPT Chat with Local and Network Data Grid
Initially the software was created for to interchange Files with the Chat Bot in TXT format.
The backend is GPT4ALL 2.7.0 and 2.7.1.
The replication of data in memory is using Apache Ignite 2.16.0
The development is completely in Java 8 for old compatibility.
The UI interface was developed in JavaFX because the viewer has support for HTML and SVG, it interface can be hide by configuration.
Localdocs using Apache PDFBox, we have support only for PDF in MeshGrid-GPT 1.0.0.
The DataGrid is enable configuring the Cache name  in the UI Interface.
The data can be cipher for in transit information creating a PKCS12/PFX KeyStore and share it with the differents nodes, the name of the keystore must be <cache_name>.pfx
The data for in transit data is cipher with AES using part of the public key of the Alias <cache> in the keystore.
The models tested and compatibles with MeshGrid are 7B GGUF, the UI provide a interface with a list of top download models from Hugging Face, and is possible download each one with double click.
The Status Panel in the console is a log of local events and show the Nodes of the Cache with the LLM active.
The Chat Panel has a Principal, and you can to add more chat and change the name for either of it

