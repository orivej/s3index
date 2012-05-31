<html>
    <head>
       <title>$title$</title>
    </head>
    <body>
		<table border=1>
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
	</body>
</html>