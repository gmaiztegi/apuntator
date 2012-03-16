define [
    'use!marionette'
    'jquery'
    'collections/FileList'
    'views/FileTable'
    'iframe-transport'
], (Marionette, $,  FileList, FileTable) ->
    
    class FilesView extends Marionette.Layout
            
        template: '#tmpl-file-main'
        
        regions: {
            table: '#file-table'
        }
        
        events:
            'submit form': 'newsend'
            'change #fileinput' : 'selectfile'
        
        initialize: ->
            @files = new FileList
            @tableview = new FileTable {
                collection: @files
            }

        selectfile: (event) =>
            @$('.file').hide()
            @$('.data').show()
            filelist = event.target.files
            file = filelist.item(0)
            @$('#filename').text file.name
        
        newsend: (event) =>
            $.ajax(event.target.action, {
                data: $(event.target).serializeArray()
                files: @$(':file')
                processData: false
                type: 'POST'
                iframe: true
                dataType: 'json'
            }).done( (data) =>
                @files.add(data)
            )
            false
        
        onRender: ->
            @fileinput = @$('#fileinput')
            
            @files.fetch()
            
            @table.show @tableview, 'append'
            
    new FilesView