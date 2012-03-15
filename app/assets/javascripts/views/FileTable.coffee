define [
    'backbone'
    'collections/FileList'
    'views/FileView'
], (Backbone, FileList, FileView) ->
    class FileTable extends Backbone.Marionette.CollectionView
        itemView: FileView