define ['backbone', 'ApuntatorApp', 'views/FilesView', 'views/RegisterView'],
(Backbone, App, FilesView, RegisterView) ->
    class Router extends Backbone.Marionette.AppRouter
        appRoutes:
            '': 'filesAction'
            'files': 'filesAction'
            'register': 'registerAction'
            '*default': 'defaultAction'
        
        controller:
            filesAction: ->
                App.main.show(FilesView)

            registerAction: ->
                App.main.show(RegisterView)

            defaultAction: ->
