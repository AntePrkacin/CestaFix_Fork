import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import { useMapEvents } from 'react-leaflet/hooks';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { NovaPrijava } from './createReport.js';

const Content = () => {

   const [markers, setMarkers] = useState([]);
   const markerIcon = new L.Icon({
       iconUrl: require("./images/R.png"),
       iconSize: [35, 35],
   });

   async function fetchMarkers() {
       const response = await fetch('/api/problems/getAllProblems', {
           method: 'GET',
           headers: {
               'Content-Type': 'application/json',
           },
       });

       if (!response.ok) {
           throw new Error('Network response was not OK');
       }

       const data = await response.json();
       return data;
   }

   useEffect(() => {
       const fetchAndPopulateMarkers = async () => {
           const dbMarkers = await fetchMarkers();
           setMarkers(dbMarkers.map((marker) => ({
               position: [marker.latitude, marker.longitude],
               popup: marker.status,
               icon: markerIcon
           })));
       };

       fetchAndPopulateMarkers();
   }, []);

   function AddMarker() {
       useMapEvents({
           click(e) {
               setMarkers([...markers, { position: [e.latlng.lat, e.latlng.lng], popup: "Placeholder prijava", icon: markerIcon }]);
               NovaPrijava();
           }
       });

       return null;
   }

   return (
       <main className='flex-grow w-full'>
           <MapContainer center={[45.812915, 15.975522]} zoom={13} className='w-full h-full'>
               <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  url="https://api.maptiler.com/maps/basic-v2/256/{z}/{x}/{y}.png?key=jl7SF9AkX5d5T6Di7nm2"
               />
               {markers.map((marker, index) => (
                  <Marker key={index} position={marker.position} icon={marker.icon}>
                      <Popup>{marker.popup}</Popup>
                  </Marker>
               ))}
               <AddMarker />
           </MapContainer>
       </main>
   );
}

export default Content;
