define [
    'backbone'
    ], (Backbone) ->
        class File extends Backbone.Model
            urlRoot: 'api/files'