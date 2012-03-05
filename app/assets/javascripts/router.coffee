define ['jQuery', 'Underscore', 'Backbone', 'views/AppView', 'views/RegisterView'],
($, _, Backbone, AppView, RegisterView) ->
    class AppRouter extends Backbone.Router
        routes:
            '': 'filesAction'
            'files': 'filesAction'
            'register': 'registerAction'
            '*default': 'defaultAction'
        
        filesAction: ->
            RegisterView.undelegateEvents()
            AppView.render()
            AppView.delegateEvents()
        
        registerAction: ->
            AppView.undelegateEvents()
            RegisterView.render()
            RegisterView.delegateEvents()
        
        defaultAction: ->
            console.log 'No route!'
    
    initialize = ->
        app_router = new AppRouter
        Backbone.history.start()
    
    {
        initialize: initialize
    }