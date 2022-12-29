import React, {useState} from "react";
import {NavLink, useParams} from "react-router-dom";
import {SearchTableResult, SelectTable} from "./SelectTable";
import {DependentTables} from "./DependentTables";
import FilterForm from "./FilterForm";
import RowCounts from "./RowCounts";
import {Dependency} from "./Dependency";

export default function DownloadAdd() {
    const routeParams = useParams();
    const datasetName = routeParams['datasetName']
    const [tableSelections, setTableSelections] = useState<SearchTableResult[]>([]);
    const [dependency, setDependency] = useState<Dependency | null>(null);
    const [queryValues, setQueryValues] = useState<any>({})
    const [dependencyChangeNumber, setDependencyChangeNumber] = useState<number>(0);
    const [searchChangeNumber, setSearchChangeNumber] = useState<number>(0);

    function whenSearchSubmitted() {
        setSearchChangeNumber(searchChangeNumber + 1);
    }

    if (!datasetName) {
        return <div>Missing Dataset</div>
    }

    return (
        <div>
            {/*Breadcrumbs*/}
            <nav aria-label="breadcrumb">
                <ol className="breadcrumb">
                    <li className="breadcrumb-item"><NavLink to="/">Home</NavLink></li>
                    <li className="breadcrumb-item active" aria-current="page">Add to: <strong>{datasetName}</strong></li>
                </ol>
            </nav>

            {/* Table Selection drop-down*/}
            <div className="mb-3">
                <label htmlFor="table-name" className="form-label">Table Name:</label>
                <SelectTable setTableSelections={setTableSelections}
                             setDependency={setDependency}
                />
            </div>

            {/*Dependent Tables*/}
            <div className="mb-3">
                <label htmlFor="table-name" className="form-label">Dependent:</label>
                <DependentTables rootTable={tableSelections.length > 0 ? tableSelections[0] : null}
                                 changeNumber={dependencyChangeNumber}
                                 setChangeNumber={setDependencyChangeNumber}
                                 dependency={dependency}
                                 setDependency={setDependency}
                />
            </div>

            {/*Filter*/}
            <FilterForm rootTable={tableSelections.length > 0 ? tableSelections[0] : null}
                        queryValues={queryValues}
                        setQueryValues={setQueryValues}
                        whenSearchSubmitted={whenSearchSubmitted}
            />

            {/*Row Counts*/}
            {dependency && queryValues && searchChangeNumber > 0 &&
                <RowCounts
                    dataset={datasetName}
                    changeNumber={searchChangeNumber}
                    dependency={dependency}
                    queryValues={queryValues}
                />}
        </div>
    );
}