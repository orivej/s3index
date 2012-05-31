<html>
    <head>
       <title>$title$</title>
    </head>
    <body>
		<table border=1>
 		<tr>
    		<th>Type</th>
    		<th>Name</th>
    		<th>Date</th>
    		<th>Size</th>
	    </tr>		
		$children:{
  		<tr>
  		$if(it.leaf)$
			$it:file()$
		$else$
			$it:dir()$
		$endif$
  		</tr>
		}$
		</table>
		<br>
		<hr>
		Generated with s3index by <a href="http://www.codeminders.com/">Codeminders</a>   
	</body>
</html>