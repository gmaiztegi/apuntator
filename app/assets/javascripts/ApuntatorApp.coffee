define ['use!marionette'], (Marionette) ->
    app = new Marionette.Application
    app.addRegions {
        main: '#content'
    }
    
    app