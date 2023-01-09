import React, {useEffect, useState} from "react";
import axios from "axios";
import {DatasetResponse} from "../models/DatasetResponse";
import {Configuration} from "../models/Configuration";
import {Dataset} from "./Dataset";

interface SiteResponse {
    hasSource: boolean;
    hasTarget: boolean;
}

export default function Datasets() {
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState(null);
    const [configuration, setConfiguration] = useState<Configuration>({hasSource: false, hasTarget: false});
    const [datasets, setDatasets] = useState<DatasetResponse[]>([]);
    const [loadingDataset, setLoadingDataset] = useState<string | null>(null);
    const [loadedDataset, setLoadedDataset] = useState<string | null>(null);
    const [loadingResult, setLoadingResult] = useState<string | null>(null);
    const [loadingError, setLoadingError] = useState<string | null>(null);

    useEffect(() => {
        axios.get<SiteResponse>('/site')
            .then(result => {
                setConfiguration({
                    hasSource: result.data.hasSource,
                    hasTarget: result.data.hasTarget,
                })
                axios.get<DatasetResponse[]>("/datasets/content")
                    .then((result) => {
                        setLoading(false);
                        setDatasets(result.data);
                    })
                    .catch(error => {
                        setLoading(false);
                        setError(error);
                    });
            })
            .catch(error => {
                setLoading(false);
                setError(error);
            });
    }, []);

    if (loading) return <div className="text-center"><i className="fa fa-spinner fa-spin"/> Loading</div>;
    if (error) return <div className="text-center"><i className="fa fa-error"/> {error}</div>;
    if (datasets.length === 0) return <div className="text-center">No Datasets</div>;

    return (
        <>
            <div className="card datasets">
                <div className="card-body">
                    <div className="row">
                        <div className="col-6 text-start">
                            <h5 className="card-title">Datasets</h5>
                        </div>
                    </div>
                    <div className="datasets p-3">
                        <div className={"row"}>
                            {datasets.map(dataset => (
                                <div key={dataset.name} className={"col-4"}>
                                    <Dataset
                                        dataset={dataset}
                                        hasDownload={configuration.hasSource}
                                        hasUpload={configuration.hasTarget}
                                        loadingDataset={loadingDataset}
                                        loadedDataset={loadedDataset}
                                        loadingResult={loadingResult}
                                        loadingError={loadingError}
                                        setLoadingDataset={setLoadingDataset}
                                        setLoadedDataset={setLoadedDataset}
                                        setLoadingResult={setLoadingResult}
                                        setLoadingError={setLoadingError}
                                    />
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </>)
}