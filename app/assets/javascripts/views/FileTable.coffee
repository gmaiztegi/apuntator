define [
    'marionette'
    'views/FileView'
], (Marionette, FileView) ->
    class FileTable extends Marionette.CollectionView
        itemView: FileView
        tagName: 'tbody'