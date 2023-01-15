import {Route, Routes} from "react-router-dom";
import AddData from "./AddData";
import Datasets from "./datasets/Datasets";
import DownloadComponent from "./download/DownloadComponent";
import DownloadBulkComponent from "./download/bulk/DownloadBulkComponent";
import DownloadModelComponent from "./download/model/DownloadModelComponent";
import VirtualFksComponent from "./vfk/VirtualFksComponent";
import EditVirtualFkComponent from "./vfk/EditVirtualFkComponent";
import Dashboard from "./dashboard/Dashboard";
import React from "react";

export default function RoutesComponent() {
    return <>
        <Routes>
            <Route path="/add/:datasetName" element=<AddData/>/>
            <Route path="/datasets" element=<Datasets/>/>
            <Route path="/download" element=<DownloadComponent/>/>
            <Route path="/download/bulk" element=<DownloadBulkComponent/>/>
            <Route path="/download/model" element=<DownloadModelComponent/>/>
            <Route path="/vfk" element=<VirtualFksComponent/>/>
            <Route path="/vfk/add" element=<EditVirtualFkComponent/>/>
            <Route path="/vfk/:pkTable/:fkName" element=<EditVirtualFkComponent/>/>
            <Route path="/" element=<Dashboard/>/>
        </Routes>
    </>
}