define [
    'use!marionette'
    'collections/FileList'
    'views/FileView'
], (Marionette, FileList, FileView) ->
    class FileTable extends Marionette.CollectionView
        itemView: FileView