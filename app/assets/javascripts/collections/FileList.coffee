define ['Backbone', 'models/File'], (Backbone, File) ->
    class FileList extends Backbone.Collection
        model: File
        url: 'api/files'
    