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
            table: '#filetable tbody'
        }
        
        events:
            'submit form': 'newsend'
            'change #fileinput' : 'selectfile'
        
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
            
            @files = new FileList
            @files.fetch()
            
            table = new FileTable {
                collection: @files
                el: @$('#file-table tbody')
            }
            
            table.render()
            @table.show table
            
    new FilesView