
function CheckReportComponent() {

 const handleSubmit = async event => {
   event.preventDefault();

   //uzmi uneseni ID
   const formData = new FormData(event.target);
   const id = formData.get('textbox');

   // prebaci stranicu na taj ID
   if(id !=="")window.location.href = `/prijava/${id}`
   
 }

 return (
   <form onSubmit={handleSubmit}>
     <label className="reportText">
       <input type="text" name="textbox" className="inputBox" placeholder="Unesite ID Prijave:"/>
     </label>
     <button type="submit" className="headerBTNSUBMIT">
       Provjeri!</button>
   </form>
 );
}



export default CheckReportComponent;