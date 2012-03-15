define ['backbone'], (Backbone) ->
    app = new Backbone.Marionette.Application
    app.addRegions {
        main: '#content'
    }
    
    app