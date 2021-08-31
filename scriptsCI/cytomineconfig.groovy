dataSource.url='jdbc:postgresql://postgresqltest:5432/docker'
//dataSource.username='docker'
//dataSource.password='docker'

cytomine.customUI.global = [
        dashboard: ["ALL"],
        search : ["ROLE_ADMIN"],
        project: ["ALL"],
        ontology: ["ROLE_ADMIN"],
        storage : ["ROLE_USER","ROLE_ADMIN"],
        activity : ["ALL"],
        feedback : ["ROLE_USER","ROLE_ADMIN"],
        explore : ["ROLE_USER","ROLE_ADMIN"],
        admin : ["ROLE_ADMIN"],
        help : ["ALL"]
]


grails.serverURL='http://localhost-core'
environments {
    test {
        grails.serverURL = "http://localhost:8090"
    }
}
grails.imageServerURL=['http://localhost-ims','http://localhost-ims2']
grails.uploadURL='http://localhost-upload'

storage_buffer='/data/images/_buffer'
storage_path='/data/images'

grails.adminPassword='password'
grails.adminPrivateKey='6f74a2f6-7748-4ca4-860c-a373f320a6f7'
grails.adminPublicKey='c070e4bf-9dfb-4e76-bb0a-e962627da952'
grails.superAdminPrivateKey='ba66c292-c468-49cf-8fee-a4e311c229fa'
grails.superAdminPublicKey='61da173f-5adc-45a7-9476-fd0508178eec'
grails.ImageServerPrivateKey='2fd0480e-0d83-453b-b4fc-b75da8fed1a2'
grails.ImageServerPublicKey='e87916c4-549c-4806-8605-f756e646d778'
grails.rabbitMQPrivateKey='784146cb-dcef-433c-97a1-a417625e9f5c'
grails.rabbitMQPublicKey='61bffe1e-cbba-45d8-b7bf-b5f24ef2e0fd'

grails.notification.email='your.email@gmail.com'
grails.notification.password='passwd'
grails.notification.smtp.host='smtp.gmail.com'
grails.notification.smtp.port='587'
grails.admin.email='info@cytomine.coop'

grails.mongo.host = 'mongodbtest'
grails.mongo.options.connectionsPerHost=10
grails.mongo.options.threadsAllowedToBlockForConnectionMultiplier=5

grails.messageBrokerServerURL='rabbitmqtest:5672'

grails.serverID='44a44a2a-b4c8-45e9-a406-1bfc21727287'

grails.mongo.host="mongodbtest"
grails.mongo.port=27017

grails.cache.enabled=false